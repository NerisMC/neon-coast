package com.neoncoast.vehicle;

import com.neoncoast.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Road cars are fast on tarmac and miserable off-road. */
public record CarSurface(float grip, float top, float accel) {
    public static final CarSurface ROAD = new CarSurface(0.95f, 1.00f, 1.00f);
    public static final CarSurface HARD = new CarSurface(0.85f, 0.85f, 0.90f);
    public static final CarSurface DIRT = new CarSurface(0.65f, 0.55f, 0.70f);
    public static final CarSurface SAND = new CarSurface(0.45f, 0.40f, 0.45f);
    public static final CarSurface SNOW = new CarSurface(0.30f, 0.50f, 0.45f);
    public static final CarSurface ICE = new CarSurface(0.07f, 0.90f, 0.20f);

    public static CarSurface of(CarEntity car) {
        BlockState feet = car.level().getBlockState(car.blockPosition());
        if (feet.is(Blocks.SNOW)) return SNOW;
        BlockState s = car.level().getBlockState(BlockPos.containing(car.getX(), car.getY() - 0.2, car.getZ()));
        if (s.is(ModBlocks.ASPHALT) || s.is(ModBlocks.ROAD_LINE) || s.is(ModBlocks.SIDEWALK)) return ROAD;
        if (s.is(BlockTags.ICE)) return ICE;
        if (s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)) return SAND;
        if (s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) return SNOW;
        if (s.is(BlockTags.DIRT) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND) || s.is(BlockTags.LEAVES)) return DIRT;
        return HARD;
    }
}
