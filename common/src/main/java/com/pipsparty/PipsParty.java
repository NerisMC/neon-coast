package com.pipsparty;

import com.pipsparty.entity.AnimatronicEntity;
import com.pipsparty.entity.Character;
import com.pipsparty.registry.ModBlocks;
import com.pipsparty.registry.ModEntities;
import com.pipsparty.registry.ModFeatures;
import com.pipsparty.registry.ModItems;
import com.pipsparty.registry.ModSounds;
import com.pipsparty.registry.ModTabs;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared entry point. Each loader calls {@link #init()} first, then registers everything in {@code Reg.ALL}. */
public final class PipsParty {
    public static final String MOD_ID = "pipsparty";
    public static final Logger LOG = LoggerFactory.getLogger("Pip's Party Palace");

    private PipsParty() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Declares every registry entry (loading the classes fills Reg.ALL). Safe to call more than once. */
    public static void init() {
        ModSounds.init();
        ModBlocks.init();
        ModEntities.init();
        ModItems.init();
        ModTabs.init();
        ModFeatures.init();
    }

    /** Default attributes for each living entity type (call after registration). */
    public static Map<EntityType<? extends LivingEntity>, AttributeSupplier> attributes() {
        Map<EntityType<? extends LivingEntity>, AttributeSupplier> map = new LinkedHashMap<>();
        for (Character c : Character.values()) {
            EntityType<AnimatronicEntity> type = ModEntities.of(c).get();
            map.put(type, AnimatronicEntity.createAttributes(c).build());
        }
        return map;
    }
}
