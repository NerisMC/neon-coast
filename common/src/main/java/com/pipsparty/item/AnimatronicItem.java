package com.pipsparty.item;

import com.pipsparty.entity.AnimatronicEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Places an animatronic. Where you put it becomes its stage spot (it comes back here every morning). */
public class AnimatronicItem extends Item {
    private final Supplier<EntityType<AnimatronicEntity>> type;

    public AnimatronicItem(Properties settings, Supplier<EntityType<AnimatronicEntity>> type) {
        super(settings);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
        AnimatronicEntity a = type.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (a == null) return InteractionResult.FAIL;
        float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() + 180.0f : 0.0f;
        a.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0.0f);
        a.setYHeadRot(yaw);
        a.setYBodyRot(yaw);
        a.setHome(at, yaw);
        level.addFreshEntity(a);
        ctx.getItemInHand().consume(1, ctx.getPlayer());
        return InteractionResult.CONSUME;
    }
}
