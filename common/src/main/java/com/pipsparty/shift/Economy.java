package com.pipsparty.shift;

import com.pipsparty.entity.AnimatronicEntity;
import com.pipsparty.registry.ModBlocks;
import com.pipsparty.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Running the pizzeria: guests come for the attractions. Each in-game day the Register earns
 * (4 + fun rating / 3) Pip Coins, up to 7 days banked. Animatronics on stage are the biggest draw...
 * and a busier pizzeria is a more haunted one at night.
 */
public final class Economy {
    private Economy() {
    }

    /** Fun rating of everything within 24 blocks. */
    public static int funRating(ServerLevel level, BlockPos center) {
        double score = 0;
        int balls = 0, chairs = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-24, -4, -24), center.offset(24, 8, 24))) {
            BlockState s = level.getBlockState(p);
            if (s.isAir()) continue;
            Block b = s.getBlock();
            if (b == ModBlocks.ARCADE_CABINET.get()) score += 4;
            else if (b == ModBlocks.PRIZE_COUNTER.get() || b == ModBlocks.PIZZA_OVEN.get()) score += 3;
            else if (b == ModBlocks.PARTY_TABLE.get() || b == ModBlocks.TICKET_MACHINE.get()) score += 2;
            else if (b == ModBlocks.PARTY_CHAIR.get() && chairs++ < 40) score += 0.5;
            else if (b == ModBlocks.BALL_PIT.get() && balls++ < 30) score += 0.5;
            else if (b == ModBlocks.PARTY_BALLOONS.get() || b == ModBlocks.GIFT_BOX.get() || b == ModBlocks.POSTER_PIP.get()
                    || b == ModBlocks.POSTER_RUSTY.get() || b == ModBlocks.POSTER_MARLA.get() || b == ModBlocks.POSTER_GNASH.get()) score += 1;
            else if (b == ModBlocks.STAGE_FLOOR.get()) score += 0.2;
        }
        int bots = level.getEntitiesOfClass(AnimatronicEntity.class, new AABB(center).inflate(24.0), a -> a.getHome() != null).size();
        return (int) Math.round(score + bots * 8);
    }

    public static void collect(ServerLevel level, BlockPos pos, ServerPlayer player) {
        ShiftData data = ShiftData.get(level.getServer());
        String key = level.dimension().identifier() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        long today = level.getOverworldClockTime() / 24000L;
        long last = data.lastCollected(key, today - 1);
        long days = Math.min(7, today - last);
        int rating = funRating(level, pos);
        if (days <= 0) {
            player.sendSystemMessage(Component.literal("Register's empty. Come back tomorrow. (Fun rating: " + rating + ")").withStyle(ChatFormatting.GRAY));
            return;
        }
        int coins = (int) (days * (4 + rating / 3));
        int guests = (int) (days * (10 + rating * 2));
        data.setLastCollected(key, today);
        while (coins > 0) {
            int n = Math.min(64, coins);
            ItemStack stack = new ItemStack(ModItems.PIP_COIN.get(), n);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            coins -= n;
        }
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.3f);
        player.sendSystemMessage(Component.literal(days + " day" + (days > 1 ? "s" : "") + " of business: " + guests + " guests, fun rating "
                + rating + ", +" + (int) (days * (4 + rating / 3)) + " Pip Coins").withStyle(ChatFormatting.GOLD));
    }
}
