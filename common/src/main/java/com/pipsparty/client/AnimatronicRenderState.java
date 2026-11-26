package com.pipsparty.client;

import com.pipsparty.entity.Character;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class AnimatronicRenderState extends LivingEntityRenderState {
    public Character character = Character.PIP;
    public int mode;
    public boolean scaring;
    public boolean ridden;
}
