package com.neoncoast.client;

import com.neoncoast.NeonCoast;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class CarRenderer extends MobRenderer<CarEntity, CarRenderState, EntityModel<CarRenderState>> {
    private final Identifier[] textures;
    private final Identifier flash;

    public CarRenderer(EntityRendererProvider.Context ctx, EntityModel<CarRenderState> model, String name, boolean police) {
        super(ctx, model, 1.2f);
        if (police) {
            textures = new Identifier[]{NeonCoast.id("textures/entity/" + name + ".png")};
            flash = NeonCoast.id("textures/entity/" + name + "_flash.png");
        } else {
            textures = new Identifier[CarEntity.COLORS.length];
            for (int i = 0; i < textures.length; i++) {
                textures[i] = NeonCoast.id("textures/entity/" + name + "_" + CarEntity.COLORS[i] + ".png");
            }
            flash = null;
        }
    }

    @Override
    public CarRenderState createRenderState() {
        return new CarRenderState();
    }

    @Override
    public void extractRenderState(CarEntity car, CarRenderState state, float td) {
        super.extractRenderState(car, state, td);
        state.steer = car.getSteerAngle(td);
        state.wheelSpin = car.getWheelSpin(td);
        state.roll = car.getRoll(td);
        state.bodyPitch = car.getBodyPitch(td);
        state.variant = car.getVariant();
        state.sirenFlash = car.isSirenOn() && (car.tickCount / 4) % 2 == 0;
    }

    @Override
    public Identifier getTextureLocation(CarRenderState state) {
        if (flash != null) {
            return state.sirenFlash ? flash : textures[0];
        }
        return textures[Math.max(0, Math.min(state.variant, textures.length - 1))];
    }
}
