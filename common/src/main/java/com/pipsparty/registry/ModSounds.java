package com.pipsparty.registry;

import com.pipsparty.PipsParty;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final Reg<SoundEvent> JUMPSCARE = sound("jumpscare");
    public static final Reg<SoundEvent> SERVO_STEP = sound("servo_step");
    public static final Reg<SoundEvent> DOOR_SLAM = sound("door_slam");
    public static final Reg<SoundEvent> DOOR_BANG = sound("door_bang");
    public static final Reg<SoundEvent> LIGHT_BUZZ = sound("light_buzz");
    public static final Reg<SoundEvent> MUSIC_BOX = sound("music_box");
    public static final Reg<SoundEvent> CHIME_6AM = sound("chime_6am");
    public static final Reg<SoundEvent> CAMERA_FLIP = sound("camera_flip");
    public static final Reg<SoundEvent> POWER_DOWN = sound("power_down");
    public static final Reg<SoundEvent> STAGE_BAND = sound("stage_band");
    public static final Reg<SoundEvent> GATOR_RUN = sound("gator_run");
    public static final Reg<SoundEvent> GIGGLE = sound("giggle");

    private ModSounds() {
    }

    private static Reg<SoundEvent> sound(String name) {
        return Reg.<SoundEvent>of(Registries.SOUND_EVENT, PipsParty.id(name), key -> SoundEvent.createVariableRangeEvent(PipsParty.id(name)));
    }

    public static void init() {
    }
}
