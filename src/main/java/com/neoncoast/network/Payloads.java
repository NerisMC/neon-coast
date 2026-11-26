package com.neoncoast.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class Payloads {
    private Payloads() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(WantedSyncPayload.ID, WantedSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MoneySyncPayload.ID, MoneySyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(JobSyncPayload.ID, JobSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenShopPayload.ID, OpenShopPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BuyPayload.ID, BuyPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CarCrashPayload.ID, CarCrashPayload.CODEC);
    }
}
