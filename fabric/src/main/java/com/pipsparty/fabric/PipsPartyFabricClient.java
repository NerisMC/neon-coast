package com.pipsparty.fabric;

import com.pipsparty.client.PipsClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class PipsPartyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PipsClient.layers().forEach((loc, def) -> ModelLayerRegistry.registerModelLayer(loc, def::get));
        PipsClient.registerRenderers(new PipsClient.RendererSink() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                EntityRendererRegistry.register(type, provider);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(PipsClient::tick);
        HudElementRegistry.addLast(PipsClient.HUD_ID, PipsClient::renderHud);
    }
}
