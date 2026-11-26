package com.neoncoast.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Cities: the world is split into 256x256 regions and about 1 in 6 is a city. Inside a city region
 * every third chunk (in both directions) builds a 48x48 district centred on itself, and the
 * districts tile into one big connected street grid at sea level + 1.
 */
public class CityFeature extends Feature<NoneFeatureConfiguration> {
    public CityFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public static boolean isCityRegion(long seed, int blockX, int blockZ) {
        long rx = Math.floorDiv(blockX, 256), rz = Math.floorDiv(blockZ, 256);
        long h = seed + rx * 0x9E3779B97F4A7C15L + rz * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return Math.floorMod(h, 6) == 0;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        if (Math.floorMod(cx, 3) != 0 || Math.floorMod(cz, 3) != 0) return false;
        int x0 = (cx << 4) - CityGenerator.OFF;
        int z0 = (cz << 4) - CityGenerator.OFF;
        if (!isCityRegion(world.getSeed(), x0 + 24, z0 + 24)) return false;
        int street = world.getSeaLevel() + 1;
        int ground = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x0 + 24, z0 + 24) - 1;
        BlockPos centre = new BlockPos(x0 + 24, ground, z0 + 24);
        if (!world.getFluidState(centre).isEmpty() || ground < street - 4 || ground > street + 22) return false;
        CityGenerator.generateDistrict(world, x0, street, z0, context.random());
        return true;
    }
}
