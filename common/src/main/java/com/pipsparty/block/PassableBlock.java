package com.pipsparty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Curtains, balloons and the ball pit: you (and the animatronics) can walk through. The ball pit slows you down. */
public class PassableBlock extends Block {
    public static final MapCodec<PassableBlock> CODEC = simpleCodec(s -> new PassableBlock(s, false));
    private final boolean sticky;

    public PassableBlock(Properties settings, boolean sticky) {
        super(settings);
        this.sticky = sticky;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (sticky) entity.makeStuckInBlock(state, new Vec3(0.6, 0.5, 0.6));
    }
}
