package com.neoncoast.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Draws the driver's arms on the steering wheel (first person only; see CarRenderer). */
public class DriverArmsLayer extends RenderLayer<CarRenderState, EntityModel<CarRenderState>> {
    private final DriverArmsModel arms;

    public DriverArmsLayer(RenderLayerParent<CarRenderState, EntityModel<CarRenderState>> parent, DriverArmsModel arms) {
        super(parent);
        this.arms = arms;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, CarRenderState state, float yRot, float xRot) {
        if (state.driverSkin == null) return;
        collector.submitModel(arms, state, pose, RenderTypes.entityCutout(state.driverSkin), light, OverlayTexture.NO_OVERLAY,
                state.outlineColor, null);
    }
}
