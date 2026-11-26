package com.neoncoast.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neoncoast.vehicle.CarEntity;
import com.neoncoast.weapon.GunItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While driving, your hands are on the wheel (DriverArmsLayer), so skip the usual floating first-person hand. */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void neoncoast$handsOnWheel(float partialTick, PoseStack pose, SubmitNodeCollector collector, LocalPlayer player,
                                        int light, CallbackInfo ci) {
        if (player.getVehicle() instanceof CarEntity car && car.getFirstPassenger() == player
                && !(player.getMainHandItem().getItem() instanceof GunItem)) {
            ci.cancel();
        }
    }
}
