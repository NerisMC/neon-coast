package com.neoncoast.crime;

import com.neoncoast.network.WantedSyncPayload;
import com.neoncoast.npc.PoliceOfficerEntity;
import com.neoncoast.vehicle.CarEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wanted level (0-5 stars) per player. Stars go up with crimes. Once no cop can see you
 * for long enough (longer at higher levels), you lose them and the stars clear.
 */
public final class WantedManager {
    public static final int MAX_STARS = 5;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        int stars;
        long lastSeen;
        boolean evading;
    }

    private WantedManager() {
    }

    public static int getStars(Player player) {
        State s = STATES.get(player.getUUID());
        return s == null ? 0 : s.stars;
    }

    public static void addStars(ServerPlayer player, int amount) {
        State s = STATES.computeIfAbsent(player.getUUID(), u -> new State());
        int before = s.stars;
        s.stars = Math.min(MAX_STARS, s.stars + amount);
        s.lastSeen = player.level().getGameTime();
        s.evading = false;
        if (s.stars != before) sync(player, s);
    }

    public static void setAtLeast(ServerPlayer player, int stars) {
        int now = getStars(player);
        if (stars > now) addStars(player, stars - now);
    }

    public static void clear(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        if (s != null && s.stars > 0) {
            s.stars = 0;
            s.evading = false;
            sync(player, s);
        }
    }

    /** Called every second from the server tick. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            State s = STATES.get(player.getUUID());
            if (s == null || s.stars == 0) continue;
            long now = player.level().getGameTime();
            if (isSeenByPolice(player)) {
                s.lastSeen = now;
                if (s.evading) {
                    s.evading = false;
                    sync(player, s);
                }
            } else {
                long hideFor = (8L + s.stars * 6L) * 20L;
                if (!s.evading && now - s.lastSeen > 60) {
                    s.evading = true;
                    sync(player, s);
                }
                if (now - s.lastSeen > hideFor) {
                    s.stars = 0;
                    s.evading = false;
                    sync(player, s);
                    player.sendOverlayMessage(Component.literal("You lost the cops.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                }
            }
            PoliceSpawner.tick(player, s.stars);
        }
    }

    private static boolean isSeenByPolice(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(40.0);
        for (Entity e : player.level().getEntities(player, area, e -> e instanceof PoliceOfficerEntity || e instanceof CarEntity car && car.isPolice())) {
            if (e instanceof PoliceOfficerEntity cop && cop.hasLineOfSight(player)) return true;
            if (e instanceof CarEntity && e.distanceToSqr(player) < 30 * 30) return true;
        }
        return false;
    }

    @Nullable
    public static Player nearestWanted(Entity from, double range) {
        Player best = null;
        double bestD = range * range;
        for (Player p : from.level().players()) {
            if (getStars(p) <= 0 || p.isSpectator() || p.isCreative()) continue;
            double d = p.distanceToSqr(from);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private static void sync(ServerPlayer player, State s) {
        ServerPlayNetworking.send(player, new WantedSyncPayload(s.stars, s.evading));
    }
}
