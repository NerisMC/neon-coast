package com.pipsparty.block;

import com.mojang.serialization.MapCodec;
import com.pipsparty.shift.FranchiseMerchant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Franchise supply catalog: spend Pip Coins on furniture, attractions, security gear and new animatronics. */
public class CatalogBlock extends PropBlock {
    public static final MapCodec<CatalogBlock> CODEC = simpleCodec(CatalogBlock::new);

    public CatalogBlock(Properties settings) {
        super(settings, 2, 0, 2, 14, 15, 14);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            new FranchiseMerchant(sp).openTradingScreen(sp, Component.literal("Franchise Supply Catalog"), 1);
        }
        return InteractionResult.SUCCESS;
    }
}
