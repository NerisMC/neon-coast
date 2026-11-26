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
        int n = ShopCatalog.NAMES.length;
        int cols = 2, rows = (n + cols - 1) / cols;
        int w = 170, h = 20, gap = 4;
        int left = (this.width - (cols * w + gap)) / 2;
        int y = this.height / 2 - (rows * (h + gap)) / 2 + 10;
        for (int i = 0; i < n; i++) {
            final int index = i;
            Component label = i == ShopCatalog.ROB
                    ? Component.literal("ROB THE STORE (+2 stars)").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    : Component.literal(ShopCatalog.NAMES[i] + " - $" + ShopCatalog.PRICES[i]);
            int col = i / rows, row = i % rows;
            this.addRenderableWidget(Button.builder(label, b -> {
                ClientPlayNetworking.send(new BuyPayload(index, shop));
                if (index == ShopCatalog.ROB) this.onClose();
            }).bounds(left + col * (w + gap), y + row * (h + gap), w, h).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int top = this.height / 2 - (((ShopCatalog.NAMES.length + 1) / 2) * 24) / 2 - 22;
        context.centeredText(this.font, this.title, this.width / 2, top, 0xFFFFD23F);
        context.centeredText(this.font, Component.literal("Cash: $" + Hud.format(ClientState.money)), this.width / 2, top + 12, 0xFF55FF55);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
