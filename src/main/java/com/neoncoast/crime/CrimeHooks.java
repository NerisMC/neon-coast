package com.neoncoast.crime;

import com.neoncoast.npc.CitizenEntity;
import com.neoncoast.npc.PoliceOfficerEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

/** Turns things you do into wanted stars. */
public final class CrimeHooks {
    private CrimeHooks() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer sp && !sp.isCreative()) {
                if (entity instanceof PoliceOfficerEntity) crime(sp, 2, "Assaulting an officer");
                else if (entity instanceof Villager || entity instanceof CitizenEntity) witnessedCrime(sp, 1, "Assault");
            }
            return InteractionResult.PASS;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer dead) {
                int stars = WantedManager.getStars(dead);
                WantedManager.clear(dead);
                long bill = Math.min(1000, Math.max(50, com.neoncoast.economy.Money.get(dead) / 10));
                com.neoncoast.economy.Money.add(dead, -bill);
                dead.sendSystemMessage(Component.literal("Hospital bill: $" + bill + (stars > 0 ? " (wanted level cleared)" : "")).withStyle(ChatFormatting.RED));
                return;
            }
            if (source.getEntity() instanceof ServerPlayer killer && !killer.isCreative()) {
                if (entity instanceof PoliceOfficerEntity) {
                    WantedManager.setAtLeast(killer, 3);
                    crime(killer, 1, "Killed an officer");
                } else if (entity instanceof Villager || entity instanceof CitizenEntity) {
                    crime(killer, 1, "Murder");
                }
            }
        });
    }

    /** A crime the police always hear about. */
    public static void crime(ServerPlayer player, int stars, String what) {
        if (player.isCreative()) return;
        WantedManager.addStars(player, stars);
        player.sendOverlayMessage(Component.literal(what + "! Wanted level: " + WantedManager.getStars(player))
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    private static final java.util.Map<java.util.UUID, Long> LAST_SHOTS = new java.util.HashMap<>();

    /** Firing a gun in public: one star if anyone sees it (at most once every 15 seconds). */
    public static void shotsFired(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = LAST_SHOTS.get(player.getUUID());
        if (last != null && now - last < 300) return;
        LAST_SHOTS.put(player.getUUID(), now);
        if (WantedManager.getStars(player) == 0) witnessedCrime(player, 1, "Shots fired");
    }

    /** A crime that only counts if a cop or a citizen saw it. */
    public static void witnessedCrime(ServerPlayer player, int stars, String what) {
        if (hasWitness(player)) crime(player, stars, what);
    }

    public static void hitWithCar(ServerPlayer driver, LivingEntity victim) {
        if (victim instanceof PoliceOfficerEntity) crime(driver, 2, "Ran over an officer");
        else if (victim instanceof Villager || victim instanceof CitizenEntity) witnessedCrime(driver, 1, "Hit and run");
    }

    private static boolean hasWitness(ServerPlayer player) {
        AABB near = player.getBoundingBox().inflate(16.0);
        AABB far = player.getBoundingBox().inflate(32.0);
        for (Entity e : player.level().getEntities(player, far, e -> e instanceof PoliceOfficerEntity || e instanceof Villager || e instanceof CitizenEntity)) {
            if (e instanceof PoliceOfficerEntity) return true;
            if (near.intersects(e.getBoundingBox()) && e instanceof LivingEntity l && l.hasLineOfSight(player)) return true;
        }
        return false;
    }
}
