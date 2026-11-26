package com.pipsparty.block;

import com.mojang.serialization.MapCodec;
import com.pipsparty.shift.Economy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The pizzeria's cash register: collect each day's takings (more fun stuff nearby = more guests = more coins). */
public class RegisterBlock extends PropBlock {
    public static final MapCodec<RegisterBlock> CODEC = simpleCodec(RegisterBlock::new);

    public RegisterBlock(Properties settings) {
        super(settings, 2, 0, 3, 14, 12, 13);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) Economy.collect(sl, pos, sp);
        return InteractionResult.SUCCESS;
    }
}
