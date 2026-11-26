package com.neoncoast;

import com.neoncoast.crime.CrimeHooks;
import com.neoncoast.crime.WantedManager;
import com.neoncoast.economy.Jobs;
import com.neoncoast.economy.Money;
import com.neoncoast.economy.ShopLogic;
import com.neoncoast.network.BuyPayload;
import com.neoncoast.network.CarCrashPayload;
import com.neoncoast.network.Payloads;
import com.neoncoast.registry.*;
import com.neoncoast.vehicle.CarEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeonCoast implements ModInitializer {
    public static final String MOD_ID = "neoncoast";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModEntities.init();
        ModBlocks.init();
        ModItems.init();
        ModItemGroup.init();
        ModWorldgen.init();
        Payloads.register();
        CrimeHooks.register();

        ServerPlayNetworking.registerGlobalReceiver(BuyPayload.ID, (payload, context) ->
                ShopLogic.buy(context.player(), payload.index(), payload.pos()));
        ServerPlayNetworking.registerGlobalReceiver(CarCrashPayload.ID, (payload, context) -> {
            if (context.player().getVehicle() instanceof CarEntity car) car.serverCrash(context.player(), payload.impact());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Money.sync(handler.player);
            Jobs.sync(handler.player);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                WantedManager.tick(server);
                Jobs.tick(server);
            }
        });
        LOGGER.info("Neon Coast loaded");
    }
}
