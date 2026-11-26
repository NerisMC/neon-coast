package com.pipsparty.neoforge;

import com.pipsparty.PipsParty;
import com.pipsparty.registry.Reg;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(PipsParty.MOD_ID)
public class PipsPartyNeoForge {
    public PipsPartyNeoForge(IEventBus modBus) {
        PipsParty.init();
        modBus.addListener(PipsPartyNeoForge::register);
        modBus.addListener(PipsPartyNeoForge::attributes);
        if (FMLEnvironment.getDist().isClient()) PipsPartyNeoForgeClient.init(modBus);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(RegisterEvent e) {
        for (Reg<?> r : Reg.ALL) {
            if (r.registry.equals(e.getRegistryKey())) e.register((ResourceKey) r.registry, r.id, r::create);
        }
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        PipsParty.attributes().forEach(e::put);
    }
}
