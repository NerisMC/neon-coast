package com.pipsparty.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pipsparty.PipsParty;
import com.pipsparty.entity.SecurityCameraEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** A little ceiling-mounted CCTV camera with a blinking red light. */
public class SecurityCameraRenderer extends EntityRenderer<SecurityCameraEntity, SecurityCameraRenderer.State> {
    private static final Identifier TEXTURE = PipsParty.id("textures/entity/security_camera.png");
    private final CamModel model;

    public static class State extends EntityRenderState {
        public float yaw, pitch;
    }

    public static class CamModel extends EntityModel<State> {
        private final ModelPart cam;

        public CamModel(ModelPart root) {
            super(root);
            this.cam = root.getChild("cam");
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            cam.xRot = s.pitch * Mth.DEG_TO_RAD;
        }
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("mount", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-0.5f, -10, -0.5f, 1, 8, 1)
                .texOffs(0, 17).addBox(-1.5f, -11, -1.5f, 3, 1, 3), PartPose.ZERO);
        root.addOrReplaceChild("cam", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, -2, -4, 4, 4, 7)
                .texOffs(0, 12).addBox(-1.5f, -1.5f, -5, 3, 3, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    public SecurityCameraRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new CamModel(ctx.bakeLayer(PipsClient.CAMERA_LAYER));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SecurityCameraEntity e, State s, float partialTick) {
        super.extractRenderState(e, s, partialTick);
        s.yaw = e.getYRot();
        s.pitch = e.getXRot();
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.0, 0.2, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - s.yaw));
        pose.scale(-1.0f, -1.0f, 1.0f);
        collector.submitModel(model, s, pose, TEXTURE, s.lightCoords, OverlayTexture.NO_OVERLAY, s.outlineColor, null);
        pose.popPose();
        super.submit(s, pose, collector, camera);
    }
}
