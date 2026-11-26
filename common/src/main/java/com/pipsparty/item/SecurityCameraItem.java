package com.pipsparty.item;

import com.pipsparty.entity.SecurityCameraEntity;
import com.pipsparty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Click a wall (or ceiling) to mount a camera looking the way you face. Rename it in an anvil to label it. */
public class SecurityCameraItem extends Item {
    public SecurityCameraItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        Direction face = ctx.getClickedFace();
        BlockPos at = ctx.getClickedPos().relative(face);
        SecurityCameraEntity cam = ModEntities.SECURITY_CAMERA.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (cam == null) return InteractionResult.FAIL;
        float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : face.toYRot();
        cam.snapTo(at.getX() + 0.5, at.getY() + 0.25, at.getZ() + 0.5, yaw, 25.0f);
        int n = level.getEntitiesOfClass(SecurityCameraEntity.class, cam.getBoundingBox().inflate(64.0), c -> true).size() + 1;
        Component name = ctx.getItemInHand().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                ? ctx.getItemInHand().getHoverName() : Component.literal("CAM " + n);
        cam.setCustomName(name);
        level.addFreshEntity(cam);
        ctx.getItemInHand().consume(1, ctx.getPlayer());
        return InteractionResult.CONSUME;
    }
}
