package com.neoncoast.vehicle;

import com.neoncoast.npc.Dialogue;
import com.neoncoast.npc.CitizenEntity;
import com.neoncoast.crime.CrimeHooks;
import com.neoncoast.crime.WantedManager;
import com.neoncoast.npc.PoliceOfficerEntity;
import com.neoncoast.registry.ModBlocks;
import com.neoncoast.registry.ModEntities;
import com.neoncoast.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A drivable car. Driving is simulated on the driver's own client (like vanilla horses and boats);
 * police cars with no player inside are driven by the server towards the nearest wanted player.
 *
 * Physics: kinematic bicycle steering (yaw rate = v * tan(steer) / wheelbase), tyre grip per surface
 * (low grip = the car slides), a 6-speed gearbox, handbrake drifts, crash damage and run-over damage.
 */
public class CarEntity extends Mob implements PlayerRideableJumping {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CAR_HEALTH = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SIREN = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final String[] COLORS = {"red", "blue", "black", "white", "yellow"};
    public static final float WHEEL_RADIUS = 0.375f;
    private static final float DEG = Mth.DEG_TO_RAD;
    private static final float[] GEARS = {0.0f, 0.12f, 0.26f, 0.42f, 0.6f, 0.8f};

    /** Set by the client entrypoint. */
    public static Consumer<Float> crashSender = f -> { };
    public static Consumer<CarEntity> soundStarter = c -> { };

    // physics (driving side)
    private float flyDist;
    private double velX, velZ;
    private float steer;
    private float throttle;
    private int handbrakeTicks;
    private float lastSpeed;
    private double lastX, lastZ;
    private int crashCooldown;
    private boolean controlledLocally;

    // visuals (every client)
    private float wheelSpin, prevWheelSpin, steerVisual, prevSteerVisual, roll, prevRoll, pitch, xRotO;
    private float displaySpeed, prevDisplaySpeed, rpm;
    private int gear = 1;
    private boolean soundRunning;

    // server
    @Nullable private UUID owner;
    private boolean civilian;
    private double serverLastX, serverLastZ;
    private int idleTicks;
    private int aiStuckTicks;
    // NPC driving
    private boolean ambientTraffic;
    private float npcHeading = Float.NaN;
    private int npcTurnCooldown, npcPanicTicks, npcStuckTicks, npcReverseTicks;

    public CarEntity(EntityType<? extends CarEntity> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createCarAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public CarType getCarType() {
        return ModEntities.carTypeOf(this.getType());
    }

    public boolean isPolice() {
        return this.getType() == ModEntities.POLICE_CAR;
    }

    // ------------------------------------------------------------------ data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(CAR_HEALTH, 100.0f);
        builder.define(SIREN, false);
    }

    public int getVariant() {
        return Mth.clamp(this.entityData.get(VARIANT), 0, COLORS.length - 1);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, v);
    }

    public float getCarHealth() {
        return this.entityData.get(CAR_HEALTH);
    }

    public void setCarHealth(float h) {
        this.entityData.set(CAR_HEALTH, Mth.clamp(h, 0.0f, getCarType().maxHealth()));
    }

    public boolean isSirenOn() {
        return this.entityData.get(SIREN);
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public void setCivilian(boolean civilian) {
        this.civilian = civilian;
    }

    public void initFresh(int variant) {
        setVariant(variant);
        setCarHealth(getCarType().maxHealth());
        setPersistenceRequired();
    }

    @Override
    public void addAdditionalSaveData(ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Variant", getVariant());
        nbt.putFloat("CarHealth", getCarHealth());
        nbt.putBoolean("Civilian", civilian);
        nbt.putBoolean("Traffic", ambientTraffic);
        nbt.storeNullable("Owner", UUIDUtil.CODEC, owner);
    }

    @Override
    public void readAdditionalSaveData(ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        setVariant(nbt.getIntOr("Variant", 0));
        setCarHealth(nbt.getFloatOr("CarHealth", getCarType().maxHealth()));
        civilian = nbt.getBooleanOr("Civilian", false);
        ambientTraffic = nbt.getBooleanOr("Traffic", false);
        owner = nbt.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.REPAIR_KIT)) {
            if (!this.level().isClientSide()) {
                setCarHealth(getCarType().maxHealth());
                held.consume(1, player);
                this.playSound(SoundEvents.ANVIL_USE, 0.6f, 1.4f);
                player.sendOverlayMessage(Component.literal("Car repaired").withStyle(ChatFormatting.GREEN));
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isSecondaryUseActive() && getFirstPassenger() instanceof CitizenEntity npc) {
            // carjacking: drag the driver out and take the wheel
            if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
                npc.stopRiding();
                npc.scare(player.position(), 300);
                npc.shout(Dialogue.pick(Dialogue.CARJACKED, this.random));
                CrimeHooks.witnessedCrime(sp, 1, "Carjacking");
                owner = player.getUUID();
                civilian = false;
                ambientTraffic = false;
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() || this.isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
            boolean mine = owner != null && owner.equals(player.getUUID());
            if (!mine) {
                if (isPolice()) {
                    CrimeHooks.crime(sp, 2, "Stole a police car");
                } else if (civilian || owner != null) {
                    CrimeHooks.witnessedCrime(sp, 1, "Car theft");
                }
                owner = player.getUUID();
                civilian = false;
            }
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (this.isRemoved()) return false;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(serverLevel, source, amount);
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            if (this.hasPassenger(player)) return false;
            if (player.getAbilities().instabuild && player.isShiftKeyDown()) {
                this.discard();
                return true;
            }
            boolean mine = owner != null && owner.equals(player.getUUID());
            if (mine && player.isShiftKeyDown()) {
                // owners can pocket their car back into a key
                this.spawnAtLocation(serverLevel, new ItemStack(ModItems.keyFor(this.getType())));
                this.discard();
                return true;
            }
            damageCar(4.0f);
            if (isPolice() && player instanceof ServerPlayer sp) CrimeHooks.crime(sp, 1, "Vandalised a police car");
            return true;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            damageCar(amount * 2.0f);
            return true;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            damageCar(amount);
            return true;
        }
        return false;
    }

    public void damageCar(float amount) {
        setCarHealth(getCarHealth() - amount);
        this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 0.6f, 0.8f + this.random.nextFloat() * 0.3f);
        if (getCarHealth() <= 0.0f) explode();
    }

    private void explode() {
        if (this.level().isClientSide() || this.isRemoved()) return;
        this.ejectPassengers();
        this.level().explode(this, getX(), getY() + 0.5, getZ(), 2.5f, Level.ExplosionInteraction.NONE);
        this.discard();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (fallDistance > 4.0f && !this.level().isClientSide()) damageCar((float) (fallDistance - 4.0) * 4.0f);
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.keyFor(this.getType()));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public boolean startRiding(Entity entity, boolean force, boolean sendEvent) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction positionUpdater) {
        super.positionRider(passenger, positionUpdater);
        if (passenger instanceof LivingEntity living) living.yBodyRot = this.getYRot();
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float g = Mth.clamp(f, -120.0f, 120.0f);
        passenger.yRotO += g - f;
        passenger.setYRot(passenger.getYRot() + g - f);
        passenger.setYHeadRot(passenger.getYRot());
    }

    // ------------------------------------------------------------------ handbrake (space bar)

    @Override
    public void onPlayerJump(int strength) {
        if (strength > 0) handbrakeTicks = 8 + strength / 6;
    }

    @Override
    public boolean canJump() {
        return true;
    }

    @Override
    public void handleStartJump(int height) {
    }

    @Override
    public void handleStopJump() {
    }

    // ------------------------------------------------------------------ driving

    @Override
    protected Vec3 getRiddenInput(Player controllingPlayer, Vec3 movementInput) {
        return Vec3.ZERO;
    }

    @Override
    protected float getRiddenSpeed(Player controllingPlayer) {
        return 0.1f;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 movementInput) {
        super.tickRidden(rider, movementInput);
        if (!this.isLocalInstanceAuthoritative()) return;
        float fwd = rider.zza > 0.01f ? 1.0f : rider.zza < -0.01f ? -1.0f : 0.0f;
        float side = rider.xxa > 0.01f ? 1.0f : rider.xxa < -0.01f ? -1.0f : 0.0f;
        float dYaw = simulate(fwd, side, true);
        if (dYaw != 0.0f) rider.setYRot(rider.getYRot() + dYaw);
    }

    /** One tick of car physics. Returns how much the car turned (to turn the driver's camera with it). */
    private float simulate(float fwdIn, float sideIn, boolean reportCrashes) {
        CarType s = getCarType();
        throttle = fwdIn;
        if (crashCooldown > 0) crashCooldown--;
        boolean onGround = this.onGround();
        double mx = getX() - lastX, mz = getZ() - lastZ;
        double moved = Math.sqrt(mx * mx + mz * mz);
        CarSurface surf = CarSurface.of(this);
        float health = getCarHealth() / s.maxHealth();
        float top = s.maxSpeed() * surf.top() * (health < 0.25f ? 0.6f : 1.0f);
        if (this.isInWater()) top *= 0.25f;

        // crash into something solid
        if (this.horizontalCollision && lastSpeed > 0.45f && moved < lastSpeed * 0.4f) {
            float impact = lastSpeed;
            if (reportCrashes && crashCooldown == 0 && this.level().isClientSide()) {
                crashCooldown = 20;
                crashSender.accept(impact);
            }
            flyDist *= 0.15f;
            velX *= 0.15;
            velZ *= 0.15;
        } else if (this.horizontalCollision && Math.abs(flyDist) > 0.05f && moved < Math.abs(flyDist) * 0.5f) {
            flyDist *= 0.5f;
        }

        if (onGround) {
            if (fwdIn > 0) {
                if (flyDist < -0.02f) {
                    flyDist = Math.min(0.0f, flyDist + s.brake());
                } else if (flyDist < top) {
                    int g = gearFor(flyDist / s.maxSpeed());
                    float pull = s.accel() * surf.accel() * (1.5f - 0.17f * g);
                    flyDist = Math.min(top, flyDist + pull * (1.0f - 0.55f * flyDist / top));
                }
            } else if (fwdIn < 0) {
                if (flyDist > 0.02f) flyDist = Math.max(0.0f, flyDist - s.brake() * (0.4f + 0.6f * surf.grip()));
                else flyDist = Math.max(-s.reverse(), flyDist - 0.012f);
            } else {
                flyDist *= 0.988f;
                flyDist -= Math.signum(flyDist) * Math.min(Math.abs(flyDist), 0.0025f);
            }
            if (flyDist > top) flyDist = Math.max(top, flyDist - 0.03f);
        }

        boolean handbrake = handbrakeTicks > 0;
        if (handbrake) {
            handbrakeTicks--;
            if (onGround) flyDist *= 0.975f;
        }

        // steering (bicycle model)
        float frac = Mth.clamp(Math.abs(flyDist) / s.maxSpeed(), 0.0f, 1.0f);
        float maxSteer = Mth.lerp(frac, s.steerLow(), s.steerHigh()) * (handbrake ? 1.6f : 1.0f);
        steer += (-sideIn * maxSteer - steer) * 0.3f;
        float dYaw = 0.0f;
        if (onGround) {
            double yawRate = flyDist * Math.tan(steer * DEG) / s.wheelbase();
            dYaw = (float) Math.toDegrees(yawRate) * (0.5f + 0.5f * surf.grip());
            if (dYaw != 0.0f) this.setYRot(this.getYRot() + dYaw);
        }

        // tyre grip: the real velocity is pulled toward where the car points
        float yawRad = this.getYRot() * DEG;
        double fx = -Mth.sin(yawRad), fz = Mth.cos(yawRad);
        if (onGround) {
            double grip = Math.min(1.0, surf.grip() * s.grip()) * (handbrake ? 0.12 : 1.0);
            velX += (fx * flyDist - velX) * grip;
            velZ += (fz * flyDist - velZ) * grip;
            double lateral = velX * -fz + velZ * fx;
            flyDist -= (float) Math.abs(lateral) * 0.03f * Math.signum(flyDist);
        } else {
            velX *= 0.99;
            velZ *= 0.99;
        }
        this.setDeltaMovement(velX, this.getDeltaMovement().y, velZ);

        lastSpeed = Math.abs(flyDist);
        lastX = getX();
        lastZ = getZ();
        return dYaw;
    }

    static int gearFor(float frac) {
        int g = 1;
        for (int i = 1; i < GEARS.length; i++) {
            if (frac >= GEARS[i]) g = i + 1;
        }
        return g;
    }

    /** Police AI and any other server-driven movement. */
    @Override
    public void travel(Vec3 movementInput) {
        if (!this.level().isClientSide() && !this.hasControllingPassenger() && getFirstPassenger() instanceof CitizenEntity npc) {
            float[] input = npcDrive(npc);
            simulate(input[0], input[1], false);
            super.travel(Vec3.ZERO);
            return;
        }
        if (!this.level().isClientSide() && !this.hasControllingPassenger() && isPolice()) {
            float[] input = policeDriver();
            simulate(input[0], input[1], false);
            super.travel(Vec3.ZERO);
            return;
        }
        if (!this.hasControllingPassenger()) {
            // coasting / parked
            flyDist *= 0.9f;
            velX *= 0.8;
            velZ *= 0.8;
            if (this.onGround()) this.setDeltaMovement(velX, this.getDeltaMovement().y, velZ);
        }
        super.travel(movementInput);
    }

    /** Returns {throttle, steer} towards the nearest wanted player. */
    private float[] policeDriver() {
        Player target = WantedManager.nearestWanted(this, 96.0);
        boolean chasing = target != null;
        if (this.entityData.get(SIREN) != chasing) this.entityData.set(SIREN, chasing);
        if (!chasing) {
            idleTicks++;
            return new float[]{0.0f, 0.0f};
        }
        idleTicks = 0;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float wanted = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float diff = Mth.wrapDegrees(wanted - getYRot());
        float steerIn = Mth.clamp(-diff / 25.0f, -1.0f, 1.0f);
        float throttleIn;
        if (dist < 7.0) {
            throttleIn = flyDist > 0.05f ? -1.0f : 0.0f;
            // close enough: officers jump out
            if (Math.abs(flyDist) < 0.15f && this.isVehicle()) this.ejectPassengers();
        } else if (Math.abs(diff) > 100.0f && dist < 20.0) {
            throttleIn = -1.0f; // target is behind us: reverse around
            steerIn = -steerIn;
        } else {
            throttleIn = 1.0f;
        }
        if (lastSpeed < 0.03f && throttleIn > 0) {
            if (++aiStuckTicks > 40) {
                handbrakeTicks = 10;
                aiStuckTicks = 0;
            }
        } else {
            aiStuckTicks = 0;
        }
        return new float[]{throttleIn, steerIn};
    }

    // ------------------------------------------------------------------ NPC drivers

    public boolean isAmbientTraffic() {
        return ambientTraffic;
    }

    public void setAmbientTraffic(boolean ambientTraffic) {
        this.ambientTraffic = ambientTraffic;
    }

    public boolean isNpcDriven() {
        return getFirstPassenger() instanceof CitizenEntity;
    }

    /** The driver got spooked (gunfire nearby): floor it. */
    public void npcPanic(int ticks) {
        npcPanicTicks = Math.max(npcPanicTicks, ticks);
    }

    public static boolean isRoad(BlockState state) {
        return state.is(ModBlocks.ASPHALT) || state.is(ModBlocks.ROAD_LINE) || state.is(ModBlocks.CROSSWALK) || state.is(ModBlocks.MANHOLE);
    }

    private boolean roadAt(double x, double z) {
        BlockPos p = BlockPos.containing(x, getY() - 0.5, z);
        return isRoad(this.level().getBlockState(p)) || isRoad(this.level().getBlockState(p.below()));
    }

    /** How many of the points 3..11 blocks along a heading (from an offset start) are road. */
    private int roadScore(float heading, double startAhead) {
        double fx = -Mth.sin(heading * DEG), fz = Mth.cos(heading * DEG);
        int n = 0;
        for (int d = 3; d <= 11; d += 2) {
            if (roadAt(getX() + fx * (startAhead + d), getZ() + fz * (startAhead + d))) n++;
        }
        return n;
    }

    /**
     * Follows the road: keeps to the right-hand lane, picks random turns at junctions, U-turns at dead ends,
     * slows for corners and stops for anyone in front. Returns {throttle, steer}.
     */
    private float[] npcDrive(CitizenEntity driver) {
        CarType s = getCarType();
        boolean panic = npcPanicTicks > 0 || driver.isScared();
        if (Float.isNaN(npcHeading)) npcHeading = Math.round(getYRot() / 90.0f) * 90.0f;
        if (npcTurnCooldown > 0) npcTurnCooldown--;

        // reversing out of a jam
        if (npcReverseTicks > 0) {
            npcReverseTicks--;
            return new float[]{-1.0f, 0.6f};
        }

        // junctions and dead ends
        int ahead = roadScore(npcHeading, 0.0);
        if (npcTurnCooldown == 0) {
            float left = npcHeading - 90.0f, right = npcHeading + 90.0f;
            boolean canLeft = roadScore(left, 0.0) >= 4;
            boolean canRight = roadScore(right, 0.0) >= 4;
            if (ahead <= 1) {
                npcHeading = canRight ? right : canLeft ? left : npcHeading + 180.0f;
                npcTurnCooldown = 50;
            } else if ((canLeft || canRight) && this.random.nextInt(100) < 3) {
                npcHeading = canRight && (!canLeft || this.random.nextBoolean()) ? right : left;
                npcTurnCooldown = 60;
            }
        }
        npcHeading = Mth.wrapDegrees(npcHeading);

        // lane keeping: find the road edges either side and aim 1.3 blocks right of the middle
        double fx = -Mth.sin(npcHeading * DEG), fz = Mth.cos(npcHeading * DEG);
        double rx = -fz, rz = fx; // right-hand side (+x is the car's left)
        int leftW = 0, rightW = 0;
        for (int i = 1; i <= 7 && roadAt(getX() - rx * i + fx * 2, getZ() - rz * i + fz * 2); i++) leftW++;
        for (int i = 1; i <= 7 && roadAt(getX() + rx * i + fx * 2, getZ() + rz * i + fz * 2); i++) rightW++;
        double offsetFromMiddle = (leftW - rightW) / 2.0;      // + means we're right of the middle
        double laneError = 1.3 - offsetFromMiddle;               // + means move further right
        if (leftW + rightW < 3) laneError = 0.0;                 // narrow / no road: don't wander
        float wantYaw = npcHeading + (float) Math.toDegrees(Math.atan2(laneError, 7.0)); // more yaw = turning right
        float diff = Mth.wrapDegrees(wantYaw - getYRot());
        float steerIn = Mth.clamp(-diff / 20.0f, -1.0f, 1.0f);

        // speed: cruise, slow for corners, brake for obstacles
        float cruise = s.maxSpeed() * (panic ? 0.8f : 0.36f);
        if (Math.abs(diff) > 25.0f) cruise *= 0.45f;
        if (obstacleAhead()) cruise = 0.0f;
        float throttleIn;
        if (flyDist < cruise - 0.02f) throttleIn = 1.0f;
        else if (flyDist > cruise + 0.08f) throttleIn = -1.0f;
        else throttleIn = 0.0f;

        // stuck against something
        if (cruise > 0.05f && lastSpeed < 0.02f) {
            if (++npcStuckTicks > 50) {
                npcStuckTicks = 0;
                npcReverseTicks = 25;
                npcHeading += 180.0f;
            }
        } else {
            npcStuckTicks = 0;
        }
        return new float[]{throttleIn, steerIn};
    }

    private boolean obstacleAhead() {
        float yawRad = getYRot() * DEG;
        double fx = -Mth.sin(yawRad), fz = Mth.cos(yawRad);
        double cx = getX() + fx * 4.5, cz = getZ() + fz * 4.5;
        AABB box = new AABB(cx - 1.6, getY(), cz - 1.6, cx + 1.6, getY() + 2.0, cz + 1.6).expandTowards(fx * 2.0, 0, fz * 2.0);
        return !this.level().getEntities(this, box, e -> (e instanceof LivingEntity || e instanceof CarEntity)
                && !this.hasPassenger(e) && e.getVehicle() != this && !(e instanceof CarEntity c && c.hasPassenger(this))).isEmpty();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
        if (this.level().isClientSide()) {
            controlledLocally = this.isLocalInstanceAuthoritative() && this.hasControllingPassenger();
            clientTick();
        } else {
            serverTick();
        }
    }

    private void clientTick() {
        prevWheelSpin = wheelSpin;
        prevSteerVisual = steerVisual;
        prevRoll = roll;
        xRotO = pitch;
        prevDisplaySpeed = displaySpeed;
        CarType s = getCarType();
        double dx = getX() - xo, dz = getZ() - zo;
        float yawRad = getYRot() * DEG;
        double fwd = -Mth.sin(yawRad) * dx + Mth.cos(yawRad) * dz;
        wheelSpin += (float) (fwd / WHEEL_RADIUS);
        displaySpeed += ((float) Math.sqrt(dx * dx + dz * dz) - displaySpeed) * 0.35f;
        float dYawRad = Mth.wrapDegrees(getYRot() - yRotO) * DEG;
        float steerTarget = controlledLocally ? steer
                : displaySpeed > 0.03f ? (float) Math.toDegrees(Math.atan(dYawRad * s.wheelbase() / displaySpeed)) : steerVisual;
        steerVisual += (Mth.clamp(steerTarget, -35.0f, 35.0f) - steerVisual) * 0.5f;
        // body roll leans OUT of corners, nose dips under braking and squats under power
        float lateralG = (float) Math.toDegrees(Math.atan(displaySpeed * dYawRad / 0.08));
        roll += (Mth.clamp(lateralG * 0.08f, -5.0f, 5.0f) - roll) * 0.25f;
        float accel = displaySpeed - prevDisplaySpeed;
        pitch += (Mth.clamp(-accel * 120.0f, -4.0f, 4.0f) - pitch) * 0.25f;

        float frac = displaySpeed / s.maxSpeed();
        gear = gearFor(frac);
        float lo = GEARS[gear - 1];
        float hi = gear < GEARS.length ? GEARS[gear] : 1.05f;
        float rpmTarget = 0.15f + 0.85f * Mth.clamp((frac - lo) / (hi - lo), 0.0f, 1.0f);
        boolean onThrottle = controlledLocally ? throttle > 0 : accel > 0.002f;
        if (onThrottle) rpmTarget = Math.max(rpmTarget, 0.4f);
        boolean running = this.isVehicle() || isSirenOn();
        if (!running) rpmTarget = 0.0f;
        rpm += (rpmTarget - rpm) * 0.3f;
        if (running && !soundRunning) {
            soundRunning = true;
            soundStarter.accept(this);
        }

        // damage smoke and fire
        float health = getCarHealth() / s.maxHealth();
        double hx = getX() - Mth.sin(yawRad) * 1.4, hz = getZ() + Mth.cos(yawRad) * 1.4;
        if (health < 0.45f && this.random.nextFloat() < (0.5f - health)) {
            this.level().addParticle(health < 0.2f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, hx, getY() + 1.0, hz, 0.0, 0.05, 0.0);
        }
        if (health < 0.15f && this.random.nextFloat() < 0.3f) {
            this.level().addParticle(ParticleTypes.FLAME, hx, getY() + 1.0, hz, 0.0, 0.02, 0.0);
        }
        // tyre smoke when sliding / doing burnouts
        if (this.onGround() && controlledLocally && (handbrakeTicks > 0 || (throttle > 0 && displaySpeed < 0.15f && gear == 1))) {
            double rx = getX() + Mth.sin(yawRad) * 1.3, rz = getZ() - Mth.cos(yawRad) * 1.3;
            this.level().addParticle(ParticleTypes.CLOUD, rx, getY() + 0.1, rz, 0.0, 0.02, 0.0);
        }
    }

    private void serverTick() {
        this.fallDistance = 0.0;
        if (npcPanicTicks > 0) npcPanicTicks--;
        if (ambientTraffic && this.tickCount % 40 == 0 && this.level().getNearestPlayer(this, 112.0) == null) {
            for (Entity p : this.getPassengers()) p.discard();
            this.discard();
            return;
        }
        double mx = getX() - serverLastX, mz = getZ() - serverLastZ;
        double moved = this.tickCount > 2 ? Math.min(3.0, Math.sqrt(mx * mx + mz * mz)) : 0.0;
        serverLastX = getX();
        serverLastZ = getZ();

        // run people over
        if (moved > 0.3) {
            Entity driver = this.getFirstPassenger();
            AABB box = this.getBoundingBox().inflate(0.4, 0.0, 0.4);
            List<Entity> hit = this.level().getEntities(this, box,
                    e -> e instanceof LivingEntity && !(e instanceof CarEntity) && !this.hasPassenger(e) && e.isAlive());
            for (Entity e : hit) {
                LivingEntity victim = (LivingEntity) e;
                if (victim.hurtTime > 0) continue;
                DamageSource src = driver instanceof Player p ? this.damageSources().playerAttack(p) : this.damageSources().mobAttack(this);
                victim.hurt(src, (float) moved * 16.0f);
                Vec3 push = new Vec3(mx, 0.0, mz).normalize().scale(moved * 1.2);
                victim.push(push.x, 0.35, push.z);
                victim.hurtMarked = true;
                if (driver instanceof ServerPlayer sp) CrimeHooks.hitWithCar(sp, victim);
                if (moved > 0.8) damageCar(2.0f);
            }
        }

        // abandoned police cars clean themselves up
        if (isPolice() && !this.isVehicle() && idleTicks > 1200) {
            this.discard();
        }
    }

    /** Server side: rider's client reported a crash into a wall. */
    public void serverCrash(ServerPlayer driver, float impact) {
        impact = Mth.clamp(impact, 0.0f, 2.0f);
        damageCar(impact * 30.0f);
        if (impact > 0.9f) {
            driver.hurt(this.damageSources().flyIntoWall(), (impact - 0.9f) * 12.0f);
        }
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.3f, 1.6f);
    }

    // ------------------------------------------------------------------ getters for render/HUD/sound

    public float getWheelSpin(float td) {
        return Mth.lerp(td, prevWheelSpin, wheelSpin);
    }

    public float getSteerAngle(float td) {
        return Mth.lerp(td, prevSteerVisual, steerVisual);
    }

    public float getRoll(float td) {
        return Mth.lerp(td, prevRoll, roll);
    }

    public float getBodyPitch(float td) {
        return Mth.lerp(td, xRotO, pitch);
    }

    public float getSpeedKmh() {
        return displaySpeed * 72.0f;
    }

    public float getRpm() {
        return rpm;
    }

    public int getGear() {
        return gear;
    }

    public void setSoundRunning(boolean running) {
        this.soundRunning = running;
    }

    public boolean isOnRoad() {
        BlockState under = this.level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
        return under.is(ModBlocks.ASPHALT) || under.is(ModBlocks.ROAD_LINE);
    }

    static boolean isIce(BlockState s) {
        return s.is(BlockTags.ICE);
    }

    static boolean isSoftGround(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND);
    }
}
