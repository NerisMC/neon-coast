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
}
