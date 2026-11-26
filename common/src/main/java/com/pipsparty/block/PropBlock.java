package com.pipsparty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Furniture and fittings: faces the player when placed, with a smaller-than-a-block shape (centred). */
public class PropBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PropBlock> CODEC = simpleCodec(s -> new PropBlock(s, 0, 0, 0, 16, 16, 16));
    private final VoxelShape northSouth;
    private final VoxelShape eastWest;

    public PropBlock(Properties settings, double x1, double y1, double z1, double x2, double y2, double z2) {
        super(settings);
        this.northSouth = Block.box(x1, y1, z1, x2, y2, z2);
        this.eastWest = Block.box(z1, y1, x1, z2, y2, x2);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction d = state.getValue(FACING);
        return d == Direction.NORTH || d == Direction.SOUTH ? northSouth : eastWest;
    }
}
