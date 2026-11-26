package com.pipsparty.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Rarely, an old Pip's Party Palace sits out in the plains. The band is still inside. */
public class PizzeriaFeature extends Feature<NoneFeatureConfiguration> {
    public PizzeriaFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        int cx = (origin.getX() & ~15) + 8, cz = (origin.getZ() & ~15) + 8;
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz) - 1;
        BlockPos nw = new BlockPos(cx - PizzeriaGenerator.WIDTH / 2, ground, cz - PizzeriaGenerator.DEPTH / 2);
        if (!PizzeriaGenerator.siteOk(level, nw, (x, z) -> level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1)) return false;
        PizzeriaGenerator.generate(level, nw, ctx.random());
        return true;
    }
}
