package com.neoncoast.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Street furniture (vending machines, dumpsters, hydrants...). Faces the player when placed. Shapes are centred. */
public class PropBlock extends HorizontalFacingBlock {
    public static final MapCodec<PropBlock> CODEC = createCodec(s -> new PropBlock(s, 0, 0, 0, 16, 16, 16));
    private final VoxelShape northSouth;
    private final VoxelShape eastWest;

    public PropBlock(Settings settings, double x1, double y1, double z1, double x2, double y2, double z2) {
        super(settings);
        this.northSouth = Block.createCuboidShape(x1, y1, z1, x2, y2, z2);
        this.eastWest = Block.createCuboidShape(z1, y1, x1, z2, y2, x2);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction d = state.get(FACING);
        return d == Direction.NORTH || d == Direction.SOUTH ? northSouth : eastWest;
    }
}
