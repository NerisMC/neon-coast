package com.neoncoast.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** One frame's snapshot of a car for the models. */
public class CarRenderState extends LivingEntityRenderState {
    public float steer;
    public float wheelSpin;
    public float roll;
    public float bodyPitch;
    public int variant;
    public boolean sirenFlash;
    /** Skin of the local player driving in first person, or null. */
    public net.minecraft.resources.Identifier driverSkin;
    public boolean driverHoldsGun;
}
