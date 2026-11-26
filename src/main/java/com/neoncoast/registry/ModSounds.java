package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent CAR_ENGINE = register("car_engine");
    public static final SoundEvent SIREN = register("siren");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        Identifier id = NeonCoast.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
    }
}
