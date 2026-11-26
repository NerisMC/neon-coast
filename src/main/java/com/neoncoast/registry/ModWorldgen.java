package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import com.neoncoast.world.AllCityFeature;
import com.neoncoast.world.CityFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

public final class ModWorldgen {
    public static final Feature<DefaultFeatureConfig> CITY = Registry.register(Registries.FEATURE, NeonCoast.id("city"),
            new CityFeature(DefaultFeatureConfig.CODEC));
    public static final Feature<DefaultFeatureConfig> ALL_CITY = Registry.register(Registries.FEATURE, NeonCoast.id("all_city"),
            new AllCityFeature(DefaultFeatureConfig.CODEC));
    public static final RegistryKey<PlacedFeature> CITY_PLACED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, NeonCoast.id("city"));

    private ModWorldgen() {
    }

    public static void init() {
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS, BiomeKeys.SAVANNA, BiomeKeys.DESERT,
                        BiomeKeys.FOREST, BiomeKeys.BIRCH_FOREST, BiomeKeys.MEADOW, BiomeKeys.BEACH),
                GenerationStep.Feature.SURFACE_STRUCTURES, CITY_PLACED);
    }
}
