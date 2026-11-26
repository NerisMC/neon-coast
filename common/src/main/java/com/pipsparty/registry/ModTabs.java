package com.pipsparty.registry;

import com.pipsparty.PipsParty;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class ModTabs {
    public static final Reg<CreativeModeTab> MAIN = Reg.<CreativeModeTab>of(Registries.CREATIVE_MODE_TAB, PipsParty.id("main"), key ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.literal("Pip's Party Palace"))
                    .icon(() -> new ItemStack(ModItems.ANIMATRONIC_ITEMS.values().iterator().next().get()))
                    .displayItems((params, out) -> {
                        out.accept(ModItems.PIZZERIA_KIT.get());
                        for (Reg<net.minecraft.world.item.Item> r : ModItems.ANIMATRONIC_ITEMS.values()) out.accept(r.get());
                        out.accept(ModItems.CAMERA_TABLET.get());
                        out.accept(ModItems.SECURITY_CAMERA.get());
                        out.accept(ModItems.OVERRIDE_CHIP.get());
                        out.accept(ModItems.PIP_COIN.get());
                        out.accept(ModItems.PIZZA_SLICE.get());
                        out.accept(ModItems.PARTY_SODA.get());
                        for (Reg<Block> b : ModBlocks.ALL) out.accept(b.get());
                    })
                    .build());

    private ModTabs() {
    }

    public static void init() {
    }
}
