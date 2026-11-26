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
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import com.neoncoast.registry.ModBlocks;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;

public class NeonCoastClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(SedanModel.LAYER, SedanModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(SportsCarModel.LAYER, SportsCarModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(PoliceCarModel.LAYER, PoliceCarModel::getTexturedModelData);

        EntityRendererRegistry.<CarEntity>register(ModEntities.SEDAN,
                ctx -> new CarRenderer<>(ctx, new SedanModel(ctx.bakeLayer(SedanModel.LAYER)), "sedan", false));
        EntityRendererRegistry.<CarEntity>register(ModEntities.SPORTS_CAR,
                ctx -> new CarRenderer<>(ctx, new SportsCarModel(ctx.bakeLayer(SportsCarModel.LAYER)), "sports_car", false));
        EntityRendererRegistry.<CarEntity>register(ModEntities.POLICE_CAR,
                ctx -> new CarRenderer<>(ctx, new PoliceCarModel(ctx.bakeLayer(PoliceCarModel.LAYER)), "police_car", true));
        EntityRendererRegistry.register(ModEntities.POLICE_OFFICER, PoliceRenderer::new);

        HudRenderCallback.EVENT.register(Hud::render);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.translucent(), ModBlocks.OFFICE_WINDOW, ModBlocks.GLASS_CURTAIN_WALL);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), ModBlocks.TRAFFIC_LIGHT, ModBlocks.BENCH, ModBlocks.FIRE_HYDRANT);

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
