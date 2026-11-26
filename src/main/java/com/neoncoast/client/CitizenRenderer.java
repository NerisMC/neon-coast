package com.neoncoast.client;

import com.neoncoast.NeonCoast;
import com.neoncoast.npc.CitizenEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public class CitizenRenderer extends HumanoidMobRenderer<CitizenEntity, CitizenRenderState, HumanoidModel<CitizenRenderState>> {
    private static final Identifier[] SKINS = new Identifier[CitizenEntity.SKINS];

    static {
        for (int i = 0; i < SKINS.length; i++) SKINS[i] = NeonCoast.id("textures/entity/citizen/citizen_" + i + ".png");
    }

    public CitizenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public CitizenRenderState createRenderState() {
        return new CitizenRenderState();
    }

    @Override
    public void extractRenderState(CitizenEntity entity, CitizenRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = entity.getSkin();
    }

    @Override
    public Identifier getTextureLocation(CitizenRenderState state) {
        return SKINS[Math.floorMod(state.skin, SKINS.length)];
    }
}
