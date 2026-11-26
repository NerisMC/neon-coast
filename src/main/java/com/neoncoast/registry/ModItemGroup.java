package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroup {
    public static final ItemGroup MAIN = Registry.register(Registries.ITEM_GROUP, NeonCoast.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.SPORTS_KEY))
                    .displayName(Text.translatable("itemGroup." + NeonCoast.MOD_ID))
                    .entries((context, entries) -> {
                        entries.add(ModItems.CITY_KIT);
                        entries.add(ModItems.SEDAN_KEY);
                        entries.add(ModItems.SPORTS_KEY);
                        entries.add(ModItems.POLICE_KEY);
                        entries.add(ModItems.REPAIR_KIT);
                        entries.add(ModItems.PACKAGE);
                        entries.add(ModBlocks.SHOP_TERMINAL);
                        entries.add(ModBlocks.JOB_BOARD);
                        entries.add(ModBlocks.CHOP_SHOP);
                        entries.add(ModBlocks.ASPHALT);
                        entries.add(ModBlocks.ROAD_LINE);
                        entries.add(ModBlocks.SIDEWALK);
                        entries.add(ModBlocks.CROSSWALK);
                        entries.add(ModBlocks.MANHOLE);
                        entries.add(ModBlocks.CONCRETE_PANEL);
                        entries.add(ModBlocks.DARK_PANEL);
                        entries.add(ModBlocks.MARBLE);
                        entries.add(ModBlocks.BRICK_FACADE);
                        entries.add(ModBlocks.METAL_CLADDING);
                        entries.add(ModBlocks.OFFICE_WINDOW);
                        entries.add(ModBlocks.GLASS_CURTAIN_WALL);
                        entries.add(ModBlocks.ROOF_GRAVEL);
                        entries.add(ModBlocks.SHOP_AWNING);
                        entries.add(ModBlocks.GARAGE_DOOR);
                        for (net.minecraft.block.Block neon : ModBlocks.NEON) entries.add(neon);
                        entries.add(ModBlocks.VENDING_MACHINE);
                        entries.add(ModBlocks.DUMPSTER);
                        entries.add(ModBlocks.AC_UNIT);
                        entries.add(ModBlocks.FIRE_HYDRANT);
                        entries.add(ModBlocks.TRAFFIC_LIGHT);
                        entries.add(ModBlocks.BENCH);
                        entries.add(ModBlocks.TRASH_CAN);
                    })
                    .build());

    private ModItemGroup() {
    }

    public static void init() {
    }
}
