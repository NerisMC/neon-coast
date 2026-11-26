package com.pipsparty.neoforge;

import com.pipsparty.client.PipsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

final class PipsPartyNeoForgeClient {
    private PipsPartyNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> PipsClient.layers().forEach(e::registerLayerDefinition));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> PipsClient.registerRenderers(new PipsClient.RendererSink() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                e.registerEntityRenderer(type, provider);
            }
        }));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(PipsClient.HUD_ID, PipsClient::renderHud));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> PipsClient.tick(Minecraft.getInstance()));
    }
}
