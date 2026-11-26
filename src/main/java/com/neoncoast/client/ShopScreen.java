package com.neoncoast.client;

import com.neoncoast.economy.ShopCatalog;
import com.neoncoast.network.BuyPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class ShopScreen extends Screen {
    private final BlockPos shop;

    public ShopScreen(BlockPos shop) {
        super(Component.literal("24/7 Store"));
        this.shop = shop;
    }

    @Override
    protected void init() {
        int w = 220, h = 20, gap = 4;
        int x = (this.width - w) / 2;
        int y = this.height / 2 - (ShopCatalog.NAMES.length * (h + gap)) / 2 + 10;
        for (int i = 0; i < ShopCatalog.NAMES.length; i++) {
            final int index = i;
            Component label = i == ShopCatalog.ROB
                    ? Component.literal("ROB THE STORE  (+2 wanted stars)").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    : Component.literal(ShopCatalog.NAMES[i] + "  -  $" + ShopCatalog.PRICES[i]);
            this.addRenderableWidget(Button.builder(label, b -> {
                ClientPlayNetworking.send(new BuyPayload(index, shop));
                if (index == ShopCatalog.ROB) this.onClose();
            }).bounds(x, y + i * (h + gap), w, h).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int top = this.height / 2 - (ShopCatalog.NAMES.length * 24) / 2 - 22;
        context.centeredText(this.font, this.title, this.width / 2, top, 0xFFFFD23F);
        context.centeredText(this.font, Component.literal("Cash: $" + Hud.format(ClientState.money)), this.width / 2, top + 12, 0xFF55FF55);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
