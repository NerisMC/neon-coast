package com.neoncoast.economy;

import com.neoncoast.network.JobSyncPayload;
import com.neoncoast.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Delivery jobs: grab a package at a Job Board, drive it to the marked spot, get paid. */
public final class Jobs {
    private static final Map<UUID, Job> ACTIVE = new HashMap<>();

    private record Job(BlockPos target, long pay) {
    }

    private Jobs() {
    }

    public static void takeJob(ServerPlayer player) {
        Job current = ACTIVE.get(player.getUUID());
        if (current != null) {
            player.displayClientMessage(Component.literal("You already have a delivery: " + describe(player, current.target)).withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        ServerLevel world = player.serverLevel();
        float angle = world.getRandom().nextFloat() * Mth.TWO_PI;
        int dist = 120 + world.getRandom().nextInt(200);
        int x = Mth.floor(player.getX() + Mth.cos(angle) * dist);
        int z = Mth.floor(player.getZ() + Mth.sin(angle) * dist);
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos target = new BlockPos(x, y, z);
        long pay = 150 + dist;
        ACTIVE.put(player.getUUID(), new Job(target, pay));
        ItemStack box = new ItemStack(ModItems.PACKAGE);
        if (!player.getInventory().add(box)) player.drop(box, false);
        player.displayClientMessage(Component.literal("Delivery job: take the package to " + x + ", " + z + " ($" + pay + ")").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        sync(player);
    }

    /** Called once a second. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Job job = ACTIVE.get(player.getUUID());
            if (job == null) continue;
            double dx = player.getX() - (job.target.getX() + 0.5), dz = player.getZ() - (job.target.getZ() + 0.5);
            if (dx * dx + dz * dz > 6 * 6) continue;
            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.PACKAGE));
            if (slot < 0) {
                player.displayClientMessage(Component.literal("You lost the package! Job cancelled.").withStyle(ChatFormatting.RED), true);
            } else {
                player.getInventory().getItem(slot).shrink(1);
                Money.add(player, job.pay);
                player.playSound(SoundEvents.PLAYER_LEVELUP, 0.8f, 1.2f);
                player.displayClientMessage(Component.literal("Delivered! +$" + job.pay).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
            }
            ACTIVE.remove(player.getUUID());
            sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        Job job = ACTIVE.get(player.getUUID());
        ServerPlayNetworking.send(player, new JobSyncPayload(job != null, job != null ? job.target : BlockPos.ZERO));
    }

    private static String describe(ServerPlayer player, BlockPos t) {
        int d = (int) Math.sqrt(player.distanceToSqr(t.getCenter()));
        return t.getX() + ", " + t.getZ() + " (" + d + "m away)";
    }
}
