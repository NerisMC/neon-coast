package com.neoncoast.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.TransparentBlock;

/** See-through window glass for buildings. */
public class CityGlassBlock extends TransparentBlock {
    public static final MapCodec<CityGlassBlock> CODEC = simpleCodec(CityGlassBlock::new);

    public CityGlassBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends TransparentBlock> codec() {
        return CODEC;
    }
}
