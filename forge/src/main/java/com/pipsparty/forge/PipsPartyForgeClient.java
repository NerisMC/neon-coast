package com.pipsparty.forge;

import com.pipsparty.client.PipsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;

final class PipsPartyForgeClient {
    private PipsPartyForgeClient() {
    }

    static void init() {
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(e -> PipsClient.layers().forEach(e::registerLayerDefinition));
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(e -> PipsClient.registerRenderers(new PipsClient.RendererSink() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                e.registerEntityRenderer(type, provider);
            }
        }));
        AddGuiOverlayLayersEvent.BUS.addListener(e -> e.getLayeredDraw().add(PipsClient.HUD_ID, PipsClient::renderHud));
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> PipsClient.tick(Minecraft.getInstance()));
    }
}
