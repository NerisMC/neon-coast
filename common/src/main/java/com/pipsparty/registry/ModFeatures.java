package com.pipsparty.registry;

import com.pipsparty.PipsParty;
import com.pipsparty.world.PizzeriaFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModFeatures {
    public static final Reg<Feature<NoneFeatureConfiguration>> PIZZERIA = Reg.<Feature<NoneFeatureConfiguration>>of(Registries.FEATURE,
            PipsParty.id("abandoned_pizzeria"), key -> new PizzeriaFeature(NoneFeatureConfiguration.CODEC));
    public static final ResourceKey<PlacedFeature> PIZZERIA_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, PipsParty.id("abandoned_pizzeria"));

    private ModFeatures() {
    }

    public static void init() {
    }
}
