package com.pipsparty.fabric;

import com.pipsparty.PipsParty;
import net.fabricmc.api.ModInitializer;

public class PipsPartyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PipsParty.init();
    }
}
