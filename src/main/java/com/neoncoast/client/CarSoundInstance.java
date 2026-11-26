package com.neoncoast.client;

import com.neoncoast.registry.ModSounds;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** Engine note (pitch follows RPM) or police siren, following the car. */
public class CarSoundInstance extends AbstractTickableSoundInstance {
    private final CarEntity car;
    private final boolean siren;

    public CarSoundInstance(CarEntity car, boolean siren) {
        super(siren ? ModSounds.SIREN : ModSounds.CAR_ENGINE, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.siren = siren;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.2f;
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        boolean running = car.isVehicle() || car.isSirenOn();
        if (car.isRemoved() || !running) {
            if (!siren) car.setSoundRunning(false);
            this.stop();
            return;
        }
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
        if (siren) {
            this.volume = car.isSirenOn() ? 1.0f : 0.0f;
            this.pitch = 1.0f;
        } else {
            float rpm = car.getRpm();
            this.volume = 0.35f + 0.5f * rpm;
            this.pitch = Mth.clamp(car.getCarType().enginePitch() * (0.6f + 1.2f * rpm), 0.5f, 2.0f);
        }
    }
}
