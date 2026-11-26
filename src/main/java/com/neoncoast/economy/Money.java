package com.neoncoast.economy;

import com.neoncoast.network.MoneySyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class Money {
    public static final long STARTING_CASH = 500;

    private Money() {
    }

    private static MoneyState state(ServerPlayer player) {
        return player.level().getServer().overworld().getDataStorage().computeIfAbsent(MoneyState.TYPE);
    }

    public static long get(ServerPlayer player) {
        return state(player).balances.getOrDefault(player.getUUID(), STARTING_CASH);
    }

    public static void add(ServerPlayer player, long delta) {
        MoneyState s = state(player);
        long now = Math.max(0, s.balances.getOrDefault(player.getUUID(), STARTING_CASH) + delta);
        s.balances.put(player.getUUID(), now);
        s.setDirty();
        sync(player);
    }

    /** Takes the money if the player has it. */
    public static boolean spend(ServerPlayer player, long amount) {
        if (get(player) < amount) return false;
        add(player, -amount);
        return true;
    }

    public static void sync(ServerPlayer player) {
        ServerPlayNetworking.send(player, new MoneySyncPayload(get(player)));
    }
}
