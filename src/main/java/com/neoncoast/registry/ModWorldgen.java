package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import com.neoncoast.world.AllCityFeature;
import com.neoncoast.world.CityFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModWorldgen {
    public static final Feature<NoneFeatureConfiguration> CITY = Registry.register(BuiltInRegistries.FEATURE, NeonCoast.id("city"),
            new CityFeature(NoneFeatureConfiguration.CODEC));
    public static final Feature<NoneFeatureConfiguration> ALL_CITY = Registry.register(BuiltInRegistries.FEATURE, NeonCoast.id("all_city"),
            new AllCityFeature(NoneFeatureConfiguration.CODEC));
    public static final ResourceKey<PlacedFeature> CITY_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, NeonCoast.id("city"));

    private ModWorldgen() {
    }

    public static void init() {
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SAVANNA, Biomes.DESERT,
                        Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.MEADOW, Biomes.BEACH),
                GenerationStep.Decoration.SURFACE_STRUCTURES, CITY_PLACED);
    }
}
