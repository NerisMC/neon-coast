package com.neoncoast.client;

import com.neoncoast.NeonCoast;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CarRenderer<M extends EntityModel<CarEntity>> extends MobRenderer<CarEntity, M> {
    private final ResourceLocation[] textures;
    private final ResourceLocation flash;

    public CarRenderer(EntityRendererProvider.Context ctx, M model, String name, boolean police) {
        super(ctx, model, 1.2f);
        if (police) {
            textures = new ResourceLocation[]{NeonCoast.id("textures/entity/" + name + ".png")};
            flash = NeonCoast.id("textures/entity/" + name + "_flash.png");
        } else {
            textures = new ResourceLocation[CarEntity.COLORS.length];
            for (int i = 0; i < textures.length; i++) {
                textures[i] = NeonCoast.id("textures/entity/" + name + "_" + CarEntity.COLORS[i] + ".png");
            }
            flash = null;
        }
    }

    @Override
    public ResourceLocation getTexture(CarEntity car) {
        if (flash != null) {
            return car.isSirenOn() && (car.tickCount / 4) % 2 == 0 ? flash : textures[0];
        }
        return textures[Math.min(car.getVariant(), textures.length - 1)];
    }
}
