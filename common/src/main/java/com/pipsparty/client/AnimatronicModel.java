package com.pipsparty.client;

import com.pipsparty.entity.AnimatronicEntity;
import com.pipsparty.entity.Character;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * One rig for the whole band: a stocky costume body with a big head, a hinged jaw, and per-character snout,
 * ears and tail. Texture layout (64x64) is mirrored by tools/gen_assets.py, which paints the skins.
 */
public class AnimatronicModel extends EntityModel<AnimatronicRenderState> {
    private final ModelPart head, jaw, body, tail, rightArm, leftArm, rightLeg, leftLeg;

    public AnimatronicModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.body = root.getChild("body");
        this.tail = body.getChild("tail");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition create(Character c) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -7, -4, 8, 7, 8), PartPose.offset(0, 5, 0));
        boolean gator = c == Character.GNASH;
        head.addOrReplaceChild("jaw", gator
                ? CubeListBuilder.create().texOffs(0, 48).addBox(-3, 0, -7, 6, 2, 7)
                : CubeListBuilder.create().texOffs(32, 0).addBox(-3, 0, -3, 6, 2, 3), PartPose.offset(0, -1, -4));
        CubeListBuilder snout = CubeListBuilder.create().texOffs(32, 6);
        switch (c) {
            case PIP -> snout.addBox(-2, -1, -3, 4, 3, 3);
            case RUSTY -> snout.addBox(-2, -1, -4, 4, 3, 4);
            case MARLA -> snout.addBox(-2.5f, -1, -3, 5, 3, 3);
            case GNASH -> snout.addBox(-3, -1, -7, 6, 3, 7);
        }
        head.addOrReplaceChild("snout", snout, PartPose.offset(0, gator ? -4 : -3, -4));

        switch (c) {
            case PIP -> {
                head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 16).addBox(-1, -3, -0.5f, 2, 3, 1), PartPose.offsetAndRotation(-3, -7, 0, 0, 0, -0.25f));
                head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 16).mirror().addBox(-1, -3, -0.5f, 2, 3, 1), PartPose.offsetAndRotation(3, -7, 0, 0, 0, 0.25f));
            }
            case RUSTY -> {
                head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 16).addBox(-1, 0, -1.5f, 1, 5, 3), PartPose.offsetAndRotation(-4, -6, 0, 0, 0, 0.15f));
                head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 16).mirror().addBox(0, 0, -1.5f, 1, 5, 3), PartPose.offsetAndRotation(4, -6, 0, 0, 0, -0.15f));
            }
            case MARLA -> {
                head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 16).addBox(-1, -1, -0.5f, 2, 1, 1), PartPose.offset(-3, -7, 0));
                head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 16).mirror().addBox(-1, -1, -0.5f, 2, 1, 1), PartPose.offset(3, -7, 0));
            }
            case GNASH -> {   // eye bumps
                head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 16).addBox(-1.5f, -2, -1, 3, 2, 2), PartPose.offset(-2, -7, -2));
                head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 16).mirror().addBox(-1.5f, -2, -1, 3, 2, 2), PartPose.offset(2, -7, -2));
            }
        }

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-5, 0, -3, 10, 10, 6), PartPose.offset(0, 5, 0));
        switch (c) {
            case PIP -> body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(32, 32).addBox(-1.5f, -1.5f, 0, 3, 3, 9), PartPose.offsetAndRotation(0, 8, 3, -0.5f, 0, 0));
            case RUSTY -> body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(32, 32).addBox(-1, -1, 0, 2, 2, 5), PartPose.offsetAndRotation(0, 7, 3, 0.6f, 0, 0));
            case MARLA -> body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(32, 32).addBox(-1, -1, 0, 2, 2, 2), PartPose.offset(0, 8, 3));
            case GNASH -> body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(32, 32).addBox(-2, -1.5f, 0, 4, 3, 12), PartPose.offsetAndRotation(0, 8, 3, -0.9f, 0, 0));
        }

        CubeListBuilder rArm = CubeListBuilder.create().texOffs(32, 16).addBox(-1.5f, -1, -1.5f, 3, 10, 3);
        CubeListBuilder lArm = CubeListBuilder.create().texOffs(44, 16).addBox(-1.5f, -1, -1.5f, 3, 10, 3);
        if (c == Character.MARLA) {   // digging claws
            rArm.texOffs(20, 48).addBox(-2, 9, -2, 4, 2, 4);
            lArm.texOffs(20, 48).mirror().addBox(-2, 9, -2, 4, 2, 4);
        }
        root.addOrReplaceChild("right_arm", rArm, PartPose.offset(-6.5f, 6, 0));
        root.addOrReplaceChild("left_arm", lArm, PartPose.offset(6.5f, 6, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-2, 0, -2, 4, 9, 4), PartPose.offset(-2.5f, 15, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 32).addBox(-2, 0, -2, 4, 9, 4), PartPose.offset(2.5f, 15, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AnimatronicRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;

        // stiff, servo-like walk
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 1.1f * s.walkAnimationSpeed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.7f;
        leftArm.xRot = swing * 0.7f;
        tail.yRot = Mth.sin(t * 0.12f) * 0.25f;

        if (s.scaring) {
            // lunge: arms up, jaw wide, head shaking
            rightArm.xRot = -2.3f + Mth.sin(t * 2.1f) * 0.15f;
            leftArm.xRot = -2.3f - Mth.sin(t * 2.1f) * 0.15f;
            rightArm.zRot = 0.35f;
            leftArm.zRot = -0.35f;
            jaw.xRot = 0.75f;
            head.xRot = -0.25f;
            head.zRot = Mth.sin(t * 3.3f) * 0.12f;
            return;
        }
        if (s.mode == AnimatronicEntity.MODE_STAGE && s.walkAnimationSpeed < 0.05f) {
            // performing on stage: sway, wave, sing
            float beat = t * 0.25f;
            head.zRot = Mth.sin(beat) * 0.12f;
            rightArm.xRot = -0.6f + Mth.sin(beat * 2.0f) * 0.4f;
            leftArm.xRot = -0.3f - Mth.sin(beat * 2.0f) * 0.3f;
            jaw.xRot = Math.max(0.0f, Mth.sin(t * 0.9f)) * 0.35f;
        } else if (s.mode == AnimatronicEntity.MODE_HUNT || s.ridden) {
            rightArm.xRot -= 0.9f;
            leftArm.xRot -= 0.9f;
            jaw.xRot = 0.15f + Math.max(0.0f, Mth.sin(t * 0.4f)) * 0.15f;
            head.zRot = Mth.sin(t * 0.07f) * 0.18f;   // the creepy head tilt
        } else {
            // idle twitch every few seconds
            float tw = Mth.sin(t * 0.05f);
            if (tw > 0.97f) head.zRot = Mth.sin(t * 4.0f) * 0.1f;
        }
    }
}
