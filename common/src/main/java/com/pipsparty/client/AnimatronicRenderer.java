package com.pipsparty.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pipsparty.PipsParty;
import com.pipsparty.entity.AnimatronicEntity;
import com.pipsparty.entity.Character;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class AnimatronicRenderer extends MobRenderer<AnimatronicEntity, AnimatronicRenderState, AnimatronicModel> {
    private final Identifier texture;

    public AnimatronicRenderer(EntityRendererProvider.Context ctx, Character c) {
        super(ctx, new AnimatronicModel(ctx.bakeLayer(PipsClient.layer(c))), 0.7f);
        String name = c.name().toLowerCase();
        this.texture = PipsParty.id("textures/entity/" + name + ".png");
        RenderType eyes = RenderTypes.eyes(PipsParty.id("textures/entity/" + name + "_eyes.png"));
        this.addLayer(new EyesLayer<AnimatronicRenderState, AnimatronicModel>(this) {
            @Override
            public RenderType renderType() {
                return eyes;
            }
        });
    }

    @Override
    public AnimatronicRenderState createRenderState() {
        return new AnimatronicRenderState();
    }

    @Override
    public void extractRenderState(AnimatronicEntity e, AnimatronicRenderState s, float partialTick) {
        super.extractRenderState(e, s, partialTick);
        s.character = e.character;
        s.mode = e.getMode();
        s.scaring = e.getScareTarget() >= 0;
        s.ridden = e.isVehicle();
    }

    @Override
    public Identifier getTextureLocation(AnimatronicRenderState s) {
        return texture;
    }

    @Override
    protected void scale(AnimatronicRenderState s, PoseStack pose) {
        pose.scale(1.45f, 1.45f, 1.45f);
    }
}
