package com.neoncoast.client;

import com.neoncoast.NeonCoast;
import com.neoncoast.npc.PoliceOfficerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class PoliceRenderer extends HumanoidMobRenderer<PoliceOfficerEntity, HumanoidModel<PoliceOfficerEntity>> {
    private static final ResourceLocation TEXTURE = NeonCoast.id("textures/entity/police_officer.png");

    public PoliceRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTexture(PoliceOfficerEntity entity) {
        return TEXTURE;
    }
}
