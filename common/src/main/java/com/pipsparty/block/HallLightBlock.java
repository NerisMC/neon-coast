package com.pipsparty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Ceiling light outside the office. Flick it on with a Light Button to see what's in the doorway. */
public class HallLightBlock extends Block {
    public static final MapCodec<HallLightBlock> CODEC = simpleCodec(HallLightBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public HallLightBlock(Properties settings) {
        super(settings.lightLevel(s -> s.getValue(LIT) ? 15 : 0));
        this.registerDefaultState(this.getStateDefinition().any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
