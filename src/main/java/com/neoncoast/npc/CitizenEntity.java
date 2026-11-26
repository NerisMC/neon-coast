package com.neoncoast.npc;

import com.neoncoast.registry.ModBlocks;
import com.neoncoast.vehicle.CarEntity;
import com.neoncoast.weapon.GunItem;
import com.neoncoast.crime.WantedManager;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A city resident. Walks the sidewalks, chats when you talk to them (right-click), panics and runs from
 * gunfire, and can drive a car (see CarEntity's NPC driving). Ambient ones despawn when nobody is around.
 */
public class CitizenEntity extends PathfinderMob {
    public static final int SKINS = 16;
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);

    private int scaredTicks;
    private Vec3 scaredFrom = Vec3.ZERO;
    private int talkCooldown;
    private int yellCooldown;
    private boolean ambient;
    private int aloneTicks;

    public CitizenEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createCitizenAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
    }

    public int getSkin() {
        return Math.floorMod(this.entityData.get(SKIN), SKINS);
    }

    public void setAmbient(boolean ambient) {
        this.ambient = ambient;
    }

    public boolean isScared() {
        return scaredTicks > 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeGoal());
        this.goalSelector.addGoal(5, new SidewalkStrollGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Gives a fresh citizen a random look and name. */
    public void randomize() {
        this.entityData.set(SKIN, this.random.nextInt(SKINS));
        this.setCustomName(Component.literal(Dialogue.pick(Dialogue.FIRST_NAMES, this.random)));
        this.setCustomNameVisible(false);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        randomize();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Skin", getSkin());
        out.putBoolean("Ambient", ambient);
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(SKIN, in.getIntOr("Skin", 0));
        ambient = in.getBooleanOr("Ambient", false);
    }

    // ------------------------------------------------------------------ talking

    private String name() {
        return this.getCustomName() != null ? this.getCustomName().getString() : "Citizen";
    }

    /** Sends a line of speech to one player. */
    public void sayTo(ServerPlayer player, String line) {
        player.sendSystemMessage(Component.literal("<" + name() + "> ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(line).withStyle(ChatFormatting.WHITE)));
    }

    /** Shouts a line to everyone within 16 blocks. */
    public void shout(String line) {
        if (!(this.level() instanceof ServerLevel level) || yellCooldown > 0) return;
        yellCooldown = 60;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(16.0), p -> true)) {
            sayTo(p, line);
        }
    }

    private String lineFor(Player player) {
        if (isScared()) return Dialogue.pick(Dialogue.SCARED, this.random);
        if (player.getMainHandItem().getItem() instanceof GunItem) return Dialogue.pick(Dialogue.GUN, this.random);
        if (WantedManager.getStars(player) > 0 && this.random.nextBoolean()) return Dialogue.pick(Dialogue.WANTED, this.random);
        if (this.getVehicle() instanceof CarEntity) return Dialogue.pick(Dialogue.DRIVING, this.random);
        return Dialogue.pick(Dialogue.GREET, this.random);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level().isClientSide() && player instanceof ServerPlayer sp && talkCooldown <= 0) {
            talkCooldown = 20;
            this.getLookControl().setLookAt(player, 30.0f, 30.0f);
            this.getNavigation().stop();
            sayTo(sp, lineFor(player));
            if (player.getMainHandItem().getItem() instanceof GunItem) scare(player.position(), 120);
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ fear

    public void scare(Vec3 from, int ticks) {
        boolean wasCalm = scaredTicks <= 0;
        scaredTicks = Math.max(scaredTicks, ticks);
        scaredFrom = from;
        if (this.getVehicle() instanceof CarEntity car) car.npcPanic(ticks);
        if (wasCalm && this.random.nextInt(3) == 0) shout(Dialogue.pick(Dialogue.SCARED, this.random));
    }

    /** Everyone near a gunshot panics and runs. */
    public static void scareNear(ServerLevel level, Vec3 pos, double radius, @Nullable Entity cause) {
        List<CitizenEntity> list = level.getEntitiesOfClass(CitizenEntity.class, new AABB(pos, pos).inflate(radius), c -> true);
        for (CitizenEntity c : list) c.scare(pos, 200 + c.random.nextInt(100));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isAlive()) {
            Entity attacker = source.getEntity();
            scare(attacker != null ? attacker.position() : this.position(), 300);
            if (this.random.nextInt(2) == 0) shout(Dialogue.pick(Dialogue.HURT, this.random));
        }
        return hurt;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (scaredTicks > 0) scaredTicks--;
        if (talkCooldown > 0) talkCooldown--;
        if (yellCooldown > 0) yellCooldown--;
        if (ambient && this.tickCount % 20 == 0) {
            Player near = this.level().getNearestPlayer(this, 112.0);
            aloneTicks = near == null ? aloneTicks + 20 : 0;
            if (aloneTicks > 200) {
                Entity v = this.getVehicle();
                if (v instanceof CarEntity car && car.isAmbientTraffic()) car.discard();
                this.discard();
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    // ------------------------------------------------------------------ goals

    /** Runs away from whatever scared us. */
    private class FleeGoal extends Goal {
        FleeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (scaredTicks <= 0 || isPassenger()) return false;
            Vec3 pos = DefaultRandomPos.getPosAway(CitizenEntity.this, 16, 6, scaredFrom);
            return pos != null && getNavigation().moveTo(pos.x, pos.y, pos.z, 1.6);
        }

        @Override
        public boolean canContinueToUse() {
            return scaredTicks > 0 && !getNavigation().isDone();
        }
    }

    /** Wanders, preferring sidewalks and crossings over the road. */
    private class SidewalkStrollGoal extends WaterAvoidingRandomStrollGoal {
        SidewalkStrollGoal() {
            super(CitizenEntity.this, 0.75);
        }

        @Override
        public boolean canUse() {
            return !isPassenger() && scaredTicks <= 0 && super.canUse();
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            for (int i = 0; i < 10; i++) {
                Vec3 p = LandRandomPos.getPos(CitizenEntity.this, 12, 4);
                if (p == null) continue;
                BlockState below = level().getBlockState(BlockPos.containing(p.x, p.y - 0.5, p.z));
                if (below.is(ModBlocks.SIDEWALK) || below.is(ModBlocks.CROSSWALK)) return p;
            }
            return super.getPosition();
        }
    }
}
