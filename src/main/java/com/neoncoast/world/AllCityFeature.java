package com.neoncoast.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** "All City" world type: every district everywhere is city, at whatever height the flat ground is. */
public class AllCityFeature extends Feature<NoneFeatureConfiguration> {
    public AllCityFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        if (Math.floorMod(cx, 3) != 0 || Math.floorMod(cz, 3) != 0) return false;
        int x0 = (cx << 4) - CityGenerator.OFF;
        int z0 = (cz << 4) - CityGenerator.OFF;
        int street = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx << 4, cz << 4) - 1;
        CityGenerator.generateDistrict(world, x0, street, z0, context.random());
        return true;
    }
}
