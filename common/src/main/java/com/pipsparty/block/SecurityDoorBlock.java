package com.pipsparty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The office's steel shutter. Stack two in a doorway. Closed: a solid wall nothing gets through.
 * Open: rolled up out of the way (walk straight through). Use a Door Button to slam it.
 */
public class SecurityDoorBlock extends Block {
    public static final MapCodec<SecurityDoorBlock> CODEC = simpleCodec(SecurityDoorBlock::new);
    public static final BooleanProperty CLOSED = BlockStateProperties.POWERED;
    private static final VoxelShape ROLLED_UP = Block.box(0, 14, 6, 16, 16, 10);

    public SecurityDoorBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(CLOSED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLOSED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(CLOSED) ? Shapes.block() : ROLLED_UP;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(CLOSED) ? Shapes.block() : Shapes.empty();
    }
}
