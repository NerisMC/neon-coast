package com.neoncoast.npc;

import com.neoncoast.crime.WantedManager;
import com.neoncoast.economy.Money;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** Cop on foot. Ignores you unless you're wanted; at 1-2 stars a hit when you're hurt means you're busted. */
public class PoliceOfficerEntity extends PathfinderMob {
    private int noTargetTicks;

    public PoliceOfficerEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createPoliceAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 5, true, false,
                (p, level) -> p instanceof Player pl && WantedManager.getStars(pl) > 0));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (this.getTarget() instanceof Player p && WantedManager.getStars(p) <= 0) {
            this.setTarget(null);
        }
        // hop out of the cruiser once we're close
        if (this.getVehicle() instanceof CarEntity && this.getTarget() != null && this.distanceToSqr(this.getTarget()) < 64.0) {
            this.stopRiding();
        }
        if (this.getTarget() == null && !this.isPassenger()) {
            if (++noTargetTicks > 1200) this.discard(); // off duty
        } else {
            noTargetTicks = 0;
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, Entity target) {
        if (target instanceof ServerPlayer player && !player.isPassenger()) {
            int stars = WantedManager.getStars(player);
            if (stars > 0 && stars <= 2 && player.getHealth() <= 10.0f) {
                bust(player);
                return true;
            }
        }
        return super.doHurtTarget(level, target);
    }

    private void bust(ServerPlayer player) {
        long fine = Math.max(100, Money.get(player) / 5);
        Money.add(player, -fine);
        WantedManager.clear(player);
        this.setTarget(null);
        player.setHealth(Math.max(player.getHealth(), 6.0f));
        player.playSound(SoundEvents.IRON_DOOR_CLOSE, 1.0f, 0.8f);
        player.sendOverlayMessage(Component.literal("BUSTED! Fine: $" + fine).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return this.getTarget() == null && distanceSquared > 128 * 128;
    }
}
