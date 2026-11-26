package com.pipsparty.forge;

import com.pipsparty.PipsParty;
import com.pipsparty.registry.Reg;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

@Mod(PipsParty.MOD_ID)
public class PipsPartyForge {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public PipsPartyForge() {
        PipsParty.init();
        BusGroup modBus = FMLJavaModLoadingContext.get().getModBusGroup();
        RegisterEvent.getBus(modBus).addListener(e -> {
            for (Reg<?> r : Reg.ALL) {
                if (r.registry.equals(e.getRegistryKey())) e.register((ResourceKey) r.registry, r.id, r::create);
            }
        });
        EntityAttributeCreationEvent.BUS.addListener(e -> PipsParty.attributes().forEach(e::put));
        if (FMLEnvironment.dist == Dist.CLIENT) PipsPartyForgeClient.init();
    }
}
