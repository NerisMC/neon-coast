package com.pipsparty.forge;

import com.pipsparty.PipsParty;
import net.minecraftforge.fml.common.Mod;

@Mod(PipsParty.MOD_ID)
public class PipsPartyForge {
    public PipsPartyForge() {
        PipsParty.init();
    }
}
