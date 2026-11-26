package com.pipsparty.block;

import com.mojang.serialization.MapCodec;
import com.pipsparty.shift.ShiftManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Punch in here (right-click) to start your night shift. The shift runs off this block's ticks. */
public class TimeClockBlock extends PropBlock {
    public static final MapCodec<TimeClockBlock> CODEC = simpleCodec(TimeClockBlock::new);

    public TimeClockBlock(Properties settings) {
        super(settings, 3, 0, 3, 13, 16, 13);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            ShiftManager.clockIn(sl, pos, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ShiftManager.tick(level, pos);
    }
}
