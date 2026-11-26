package com.neoncoast.world;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** "All City" world type: every district everywhere is city, at whatever height the flat ground is. */
public class AllCityFeature extends Feature<DefaultFeatureConfig> {
    public AllCityFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        if (Math.floorMod(cx, 3) != 0 || Math.floorMod(cz, 3) != 0) return false;
        int x0 = (cx << 4) - CityGenerator.OFF;
        int z0 = (cz << 4) - CityGenerator.OFF;
        int street = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, cx << 4, cz << 4) - 1;
        CityGenerator.generateDistrict(world, x0, street, z0, context.getRandom());
        return true;
    }
}
