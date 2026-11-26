package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent CAR_ENGINE = register("car_engine");
    public static final SoundEvent SIREN = register("siren");
    public static final SoundEvent GUN_PISTOL = register("gun_pistol");
    public static final SoundEvent GUN_SMG = register("gun_smg");
    public static final SoundEvent GUN_RIFLE = register("gun_rifle");
    public static final SoundEvent GUN_SHOTGUN = register("gun_shotgun");
    public static final SoundEvent GUN_SNIPER = register("gun_sniper");
    public static final SoundEvent GUN_RELOAD = register("gun_reload");
    public static final SoundEvent GUN_EMPTY = register("gun_empty");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        Identifier id = NeonCoast.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
    }
}
