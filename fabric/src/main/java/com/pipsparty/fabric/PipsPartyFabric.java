package com.pipsparty.fabric;

import com.pipsparty.PipsParty;
import com.pipsparty.registry.ModFeatures;
import com.pipsparty.registry.Reg;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;

public class PipsPartyFabric implements ModInitializer {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitialize() {
        PipsParty.init();
        for (Reg<?> r : Reg.ALL) {
            Registry registry = BuiltInRegistries.REGISTRY.getValue(r.registry.identifier());
            if (registry == null) throw new IllegalStateException("Unknown registry " + r.registry);
            Registry.register(registry, r.id, r.create());
        }
        PipsParty.attributes().forEach(FabricDefaultAttributeRegistry::register);
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PipsParty.id("has_pizzeria"))),
                GenerationStep.Decoration.SURFACE_STRUCTURES, ModFeatures.PIZZERIA_PLACED);
    }
}
