package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroup {
    public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, NeonCoast.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.SPORTS_KEY))
                    .title(Component.translatable("itemGroup." + NeonCoast.MOD_ID))
                    .displayItems((context, entries) -> {
                        entries.accept(ModItems.CITY_KIT);
                        entries.accept(ModItems.SEDAN_KEY);
                        entries.accept(ModItems.SPORTS_KEY);
                        entries.accept(ModItems.POLICE_KEY);
                        entries.accept(ModItems.REPAIR_KIT);
                        entries.accept(ModItems.PACKAGE);
                        entries.accept(ModBlocks.SHOP_TERMINAL);
                        entries.accept(ModBlocks.JOB_BOARD);
                        entries.accept(ModBlocks.CHOP_SHOP);
                        entries.accept(ModBlocks.ASPHALT);
                        entries.accept(ModBlocks.ROAD_LINE);
                        entries.accept(ModBlocks.SIDEWALK);
                        entries.accept(ModBlocks.CROSSWALK);
                        entries.accept(ModBlocks.MANHOLE);
                        entries.accept(ModBlocks.CONCRETE_PANEL);
                        entries.accept(ModBlocks.DARK_PANEL);
                        entries.accept(ModBlocks.MARBLE);
                        entries.accept(ModBlocks.BRICK_FACADE);
                        entries.accept(ModBlocks.METAL_CLADDING);
                        entries.accept(ModBlocks.OFFICE_WINDOW);
                        entries.accept(ModBlocks.GLASS_CURTAIN_WALL);
                        entries.accept(ModBlocks.ROOF_GRAVEL);
                        entries.accept(ModBlocks.SHOP_AWNING);
                        entries.accept(ModBlocks.GARAGE_DOOR);
                        for (net.minecraft.world.level.block.Block neon : ModBlocks.NEON) entries.accept(neon);
                        entries.accept(ModBlocks.VENDING_MACHINE);
                        entries.accept(ModBlocks.DUMPSTER);
                        entries.accept(ModBlocks.AC_UNIT);
                        entries.accept(ModBlocks.FIRE_HYDRANT);
                        entries.accept(ModBlocks.TRAFFIC_LIGHT);
                        entries.accept(ModBlocks.BENCH);
                        entries.accept(ModBlocks.TRASH_CAN);
                    })
                    .build());

    private ModItemGroup() {
    }

    public static void init() {
    }
}
