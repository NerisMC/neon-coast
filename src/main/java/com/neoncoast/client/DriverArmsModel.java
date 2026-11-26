package com.neoncoast.client;

import com.neoncoast.NeonCoast;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The driver's own arms (with their skin and sleeves), reaching from the shoulders to the 9 and 3 o'clock
 * positions of the steering wheel and turning with it. Drawn in first person so you see your hands on the wheel.
 * Coordinates are the car model's pixel space (ground y = 24, front = -z, +x = the car's left).
 */
public class DriverArmsModel extends Model<CarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(NeonCoast.id("driver_arms"), "main");

    /**
     * @param wheelX/Y/Z  wheel centre; wheelTilt = the steering part's xRot
     * @param shoulderY   shoulder pivot height (model y); seatX/seatZ = the driver's centre line
     */
    public record Geometry(float wheelX, float wheelY, float wheelZ, float wheelTilt, float seatX, float seatZ, float shoulderY) {
    }

    private static final float GRIP = 2.5f;
    private static final float ARM_LENGTH = 10.0f;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final Geometry geo;

    public DriverArmsModel(ModelPart root, Geometry geo) {
        super(root, RenderTypes::entityCutout);
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.geo = geo;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition right = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.ZERO);
        right.addOrReplaceChild("right_sleeve",
                CubeListBuilder.create().texOffs(40, 32).addBox(-2.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.25f)), PartPose.ZERO);
        PartDefinition left = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 48).addBox(-2.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.ZERO);
        left.addOrReplaceChild("left_sleeve",
                CubeListBuilder.create().texOffs(48, 48).addBox(-2.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.25f)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(CarRenderState state) {
        super.setupAnim(state);
        float wheelSpin = -state.steer * Mth.DEG_TO_RAD * 3.0f;   // same as the car models' steering part
        float roll = state.roll * Mth.DEG_TO_RAD, pitch = state.bodyPitch * Mth.DEG_TO_RAD;
        // player's right arm is on the -x side of the driver (the car faces -z)
        aim(rightArm, geo.seatX() - 5.0f, grip(-GRIP, wheelSpin, roll, pitch));
        aim(leftArm, geo.seatX() + 5.0f, grip(GRIP, wheelSpin, roll, pitch));
        rightArm.visible = !state.driverHoldsGun;   // holding a gun out the window: one hand on the wheel
    }

    /** Wheel rim point at local x = side, after the wheel's tilt and spin and the body's roll/pitch. */
    private float[] grip(float side, float spin, float roll, float pitch) {
        // local rim point (x, 0, 0.5) -> tilt about x -> spin about z (ModelPart order: Rz * Ry * Rx)
        float x = side, y = 0.0f, z = 0.5f;
        float t = geo.wheelTilt();
        float y1 = y * Mth.cos(t) - z * Mth.sin(t), z1 = y * Mth.sin(t) + z * Mth.cos(t);
        float x2 = x * Mth.cos(spin) - y1 * Mth.sin(spin), y2 = x * Mth.sin(spin) + y1 * Mth.cos(spin);
        float px = geo.wheelX() + x2, py = geo.wheelY() + y2, pz = geo.wheelZ() + z1;
        // the body (and wheel) roll/pitch about the car's ground centre (0, 24, 0)
        float ry = py - 24.0f;
        float y3 = ry * Mth.cos(pitch) - pz * Mth.sin(pitch), z3 = ry * Mth.sin(pitch) + pz * Mth.cos(pitch);
        float x4 = px * Mth.cos(roll) - y3 * Mth.sin(roll), y4 = px * Mth.sin(roll) + y3 * Mth.cos(roll);
        return new float[]{x4, y4 + 24.0f, z3};
    }

    /** Points an arm (which hangs along +y) from its shoulder to the target and stretches it to reach. */
    private void aim(ModelPart arm, float shoulderX, float[] target) {
        arm.x = shoulderX;
        arm.y = geo.shoulderY();
        arm.z = geo.seatZ();
        float dx = target[0] - arm.x, dy = target[1] - arm.y, dz = target[2] - arm.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.01f) return;
        // R = Rz * Ry * Rx maps (0,1,0) to (sin a sin b, cos a, sin a cos b)
        arm.xRot = (float) Math.acos(Mth.clamp(dy / len, -1.0f, 1.0f));
        arm.yRot = (float) Math.atan2(dx, dz);
        arm.zRot = 0.0f;
        float s = Mth.clamp(len / ARM_LENGTH, 0.5f, 1.3f);
        arm.yScale = s;
    }
}
