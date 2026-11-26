package com.neoncoast.client;

import com.neoncoast.network.CarCrashPayload;
import com.neoncoast.network.JobSyncPayload;
import com.neoncoast.network.MoneySyncPayload;
import com.neoncoast.network.OpenShopPayload;
import com.neoncoast.network.WantedSyncPayload;
import com.neoncoast.registry.ModEntities;
import com.neoncoast.vehicle.CarEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import com.neoncoast.registry.ModBlocks;

public class NeonCoastClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(SedanModel.LAYER, SedanModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(SportsCarModel.LAYER, SportsCarModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(PoliceCarModel.LAYER, PoliceCarModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.SEDAN,
                ctx -> new CarRenderer(ctx, new SedanModel(ctx.bakeLayer(SedanModel.LAYER)), "sedan", false));
        EntityRendererRegistry.register(ModEntities.SPORTS_CAR,
                ctx -> new CarRenderer(ctx, new SportsCarModel(ctx.bakeLayer(SportsCarModel.LAYER)), "sports_car", false));
        EntityRendererRegistry.register(ModEntities.POLICE_CAR,
                ctx -> new CarRenderer(ctx, new PoliceCarModel(ctx.bakeLayer(PoliceCarModel.LAYER)), "police_car", true));
        EntityRendererRegistry.register(ModEntities.POLICE_OFFICER, PoliceRenderer::new);

        HudElementRegistry.addLast(com.neoncoast.NeonCoast.id("hud"), Hud::render);

        ClientPlayNetworking.registerGlobalReceiver(WantedSyncPayload.ID, (payload, context) -> {
            ClientState.stars = payload.stars();
            ClientState.evading = payload.evading();
        });
        ClientPlayNetworking.registerGlobalReceiver(MoneySyncPayload.ID, (payload, context) -> ClientState.money = payload.money());
        ClientPlayNetworking.registerGlobalReceiver(JobSyncPayload.ID, (payload, context) -> {
            ClientState.jobActive = payload.active();
            ClientState.jobTarget = payload.target();
        });
        ClientPlayNetworking.registerGlobalReceiver(OpenShopPayload.ID, (payload, context) ->
                context.client().setScreen(new ShopScreen(payload.pos())));

        CarEntity.crashSender = impact -> {
            if (ClientPlayNetworking.canSend(CarCrashPayload.ID)) ClientPlayNetworking.send(new CarCrashPayload(impact));
        };
        CarEntity.soundStarter = car -> {
            Minecraft.getInstance().getSoundManager().play(new CarSoundInstance(car, false));
            if (car.isPolice()) Minecraft.getInstance().getSoundManager().play(new CarSoundInstance(car, true));
        };
    }
}
