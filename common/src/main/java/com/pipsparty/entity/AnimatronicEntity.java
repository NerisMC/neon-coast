package com.pipsparty.entity;

import com.pipsparty.registry.ModItems;
import com.pipsparty.registry.ModSounds;
import com.pipsparty.shift.ShiftManager;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * An animatronic. By day it stands on its stage spot and performs. At night (free roam) it wanders the
 * pizzeria and hunts anyone it finds. During a night shift the Time Clock moves it room to room instead.
 * Players can climb inside with an Override Chip and walk it around themselves.
 */
public class AnimatronicEntity extends PathfinderMob {
    public static final int MODE_STAGE = 0, MODE_ROAM = 1, MODE_HUNT = 2, MODE_JUMPSCARE = 3, MODE_SHIFT = 4;
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(AnimatronicEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SCARE_TARGET = SynchedEntityData.defineId(AnimatronicEntity.class, EntityDataSerializers.INT);

    public final Character character;
    @Nullable private BlockPos home;
    private float homeYaw;
    private boolean shiftControlled;
    private int scareTicks;
    private int scareCooldown;
    private boolean scareKills;
    private int stepTicks;
    private int peekTicks;

    public AnimatronicEntity(EntityType<? extends PathfinderMob> type, Level level, Character character) {
        super(type, level);
        this.character = character;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes(Character c) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.MOVEMENT_SPEED, c.speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE, MODE_STAGE);
        builder.define(SCARE_TARGET, -1);
    }

    public int getMode() {
        return this.entityData.get(MODE);
    }

    private void setMode(int m) {
        if (getMode() != m) this.entityData.set(MODE, m);
    }

    /** Entity id of the player being jumpscared right now, or -1. */
    public int getScareTarget() {
        return this.entityData.get(SCARE_TARGET);
    }

    public void setHome(BlockPos pos, float yaw) {
        this.home = pos.immutable();
        this.homeYaw = yaw;
    }

    @Nullable
    public BlockPos getHome() {
        return home;
    }

    public void setShiftControlled(boolean on) {
        this.shiftControlled = on;
        this.getNavigation().stop();
        setMode(on ? MODE_SHIFT : MODE_STAGE);
    }

    public boolean isShiftControlled() {
        return shiftControlled;
    }

    /** Puts it back on its stage spot (end of a shift). */
    public void returnHome() {
        if (home != null) {
            this.snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, homeYaw, 0.0f);
            this.setYHeadRot(homeYaw);
            this.setYBodyRot(homeYaw);
            this.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ save

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (home != null) {
            out.putInt("HomeX", home.getX());
            out.putInt("HomeY", home.getY());
            out.putInt("HomeZ", home.getZ());
            out.putFloat("HomeYaw", homeYaw);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        int hy = in.getIntOr("HomeY", Integer.MIN_VALUE);
        if (hy != Integer.MIN_VALUE) {
            home = new BlockPos(in.getIntOr("HomeX", 0), hy, in.getIntOr("HomeZ", 0));
            homeYaw = in.getFloatOr("HomeYaw", 0.0f);
        }
    }

    // ------------------------------------------------------------------ goals

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new HuntGoal());
        this.goalSelector.addGoal(2, new ReturnHomeGoal());
        this.goalSelector.addGoal(3, new NightWanderGoal());
    }

    private boolean freeRoaming() {
        return !shiftControlled && !this.isVehicle() && scareTicks <= 0 && this.level().isDarkOutside();
    }

    private boolean watchedBy(Player p) {
        Vec3 look = p.getViewVector(1.0f);
        Vec3 to = this.getEyePosition().subtract(p.getEyePosition()).normalize();
        return look.dot(to) > 0.55 && p.hasLineOfSight(this);
    }

    /** At night: find a player, chase them (each character in its own way), and jumpscare on contact. */
    private class HuntGoal extends Goal {
        private Player target;

        HuntGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!freeRoaming() || scareCooldown > 0) return false;
            Player p = level().getNearestPlayer(AnimatronicEntity.this, 28.0);
            if (p == null || p.isCreative() || p.isSpectator() || p.isPassenger() && p.getVehicle() instanceof AnimatronicEntity) return false;
            if (!hasLineOfSight(p) && distanceTo(p) > 10.0) return false;
            target = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return freeRoaming() && target != null && target.isAlive() && !target.isCreative() && distanceTo(target) < 40.0;
        }

        @Override
        public void start() {
            setMode(MODE_HUNT);
            peekTicks = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
            if (getMode() == MODE_HUNT) setMode(MODE_ROAM);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            double speed = character.huntSpeed;
            boolean watched = watchedBy(target);
            switch (character) {
                case PIP -> {
                    // freezes while you look at him
                    if (watched) {
                        getNavigation().stop();
                        return;
                    }
                }
                case GNASH -> {
                    // peeks for a few seconds, then sprints
                    if (++peekTicks < 50) {
                        getNavigation().stop();
                        getLookControl().setLookAt(target, 30.0f, 30.0f);
                        return;
                    }
                    if (peekTicks == 50) level().playSound(null, getX(), getY(), getZ(), ModSounds.GATOR_RUN.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
                }
                default -> {
                }
            }
            getLookControl().setLookAt(target, 30.0f, 30.0f);
            if (tickCount % 10 == 0) getNavigation().moveTo(target, speed);
            if (distanceToSqr(target) < 2.4 && target instanceof ServerPlayer sp) jumpscare(sp, false);
        }
    }

    /** By day: walk back to the stage and perform. */
    private class ReturnHomeGoal extends Goal {
        ReturnHomeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return home != null && !shiftControlled && !isVehicle() && level().isBrightOutside();
        }

        @Override
        public void tick() {
            double d = distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            if (d > 1.0) {
                if (getNavigation().isDone()) getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.9);
                setMode(MODE_ROAM);
                if (d > 900.0) returnHome();
            } else {
                getNavigation().stop();
                setYRot(homeYaw);
                setYBodyRot(homeYaw);
                setYHeadRot(homeYaw + Mth.sin(tickCount * 0.05f) * 25.0f);
                setMode(MODE_STAGE);
            }
        }
    }

    /** At night with nobody around: shuffle between rooms near home. */
    private class NightWanderGoal extends Goal {
        NightWanderGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!freeRoaming() || getRandom().nextInt(80) != 0) return false;
            Vec3 p = DefaultRandomPos.getPos(AnimatronicEntity.this, 12, 3);
            if (p == null || (home != null && home.distToCenterSqr(p) > 30 * 30)) return false;
            setMode(MODE_ROAM);
            return getNavigation().moveTo(p.x, p.y, p.z, 0.7);
        }

        @Override
        public boolean canContinueToUse() {
            return freeRoaming() && !getNavigation().isDone();
        }
    }

    // ------------------------------------------------------------------ jumpscare

    /** Lunges into the player's face with a scream. Kills on a night shift, hurts badly in free roam. */
    public void jumpscare(ServerPlayer player, boolean kills) {
        if (scareTicks > 0 || scareCooldown > 0 || player.isCreative() || player.isSpectator()) return;
        scareTicks = 26;
        scareKills = kills;
        this.entityData.set(SCARE_TARGET, player.getId());
        setMode(MODE_JUMPSCARE);
        getNavigation().stop();
        // stand right in front of their face, looking at them, and make them look back
        Vec3 look = Vec3.directionFromRotation(0.0f, player.getYRot());
        Vec3 spot = player.position().add(look.scale(1.25));
        float yaw = player.getYRot() + 180.0f;
        this.snapTo(spot.x, player.getY(), spot.z, yaw, 0.0f);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), -12.0f);
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 9, false, false));
        this.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.JUMPSCARE.get(), SoundSource.HOSTILE, 2.0f,
                0.9f + this.random.nextFloat() * 0.2f);
    }

    private void finishJumpscare() {
        Entity e = this.level().getEntity(getScareTarget());
        this.entityData.set(SCARE_TARGET, -1);
        if (e instanceof ServerPlayer p && p.isAlive() && this.level() instanceof ServerLevel sl) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            DamageSource src = this.damageSources().mobAttack(this);
            p.hurtServer(sl, src, scareKills ? 1000.0f : 12.0f);
            if (scareKills) ShiftManager.onGuardCaught(sl, p, this);
        }
        scareCooldown = 120;
        setMode(shiftControlled ? MODE_SHIFT : MODE_ROAM);
        if (shiftControlled) returnHome();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (scareCooldown > 0) scareCooldown--;
        if (scareTicks > 0) {
            Entity e = this.level().getEntity(getScareTarget());
            if (e != null) this.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, e.getEyePosition());
            if (--scareTicks == 0) finishJumpscare();
            return;
        }
        // heavy servo footsteps
        double moved = (getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo);
        if (moved > 0.0004 && ++stepTicks >= 9) {
            stepTicks = 0;
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.SERVO_STEP.get(), SoundSource.HOSTILE, 0.7f,
                    0.8f + this.random.nextFloat() * 0.2f);
        }
        // a possessed animatronic catches whoever it walks into
        if (this.getControllingPassenger() instanceof ServerPlayer driver) {
            for (ServerPlayer p : ((ServerLevel) this.level()).getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(0.6),
                    p -> p != driver && !p.isSpectator() && !(p.getVehicle() instanceof AnimatronicEntity))) {
                jumpscare(p, ShiftManager.isGuardOnShift(p));
                break;
            }
        }
    }

    // ------------------------------------------------------------------ possession (Override Chip)

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.OVERRIDE_CHIP.get()) && !this.isVehicle()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this, true, true);
                player.sendOverlayMessage(Component.literal("You are " + character.displayName + ". Hunt the night guard. Sneak to get out.")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!this.level().isClientSide() && passenger instanceof Player) passenger.setInvisible(true);
        setMode(MODE_ROAM);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide() && passenger instanceof Player) passenger.setInvisible(false);
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 input) {
        return new Vec3(rider.xxa * 0.4, 0.0, rider.zza > 0 ? rider.zza : rider.zza * 0.4);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (float) character.huntSpeed;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        this.setYRot(rider.getYRot());
        this.yRotO = this.getYRot();
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(rider.getYHeadRot());
        this.setXRot(rider.getXRot() * 0.5f);
    }

    // ------------------------------------------------------------------ misc

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    public UUID uuid() {
        return this.getUUID();
    }
}
