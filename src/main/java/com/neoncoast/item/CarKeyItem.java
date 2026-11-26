package com.neoncoast.item;

import com.neoncoast.vehicle.CarEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Right-click the ground to park your car there. It's yours, so driving it isn't a crime. */
public class CarKeyItem extends Item {
    private final Supplier<EntityType<CarEntity>> type;

    public CarKeyItem(Properties settings, Supplier<EntityType<CarEntity>> type) {
        super(settings);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel world)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        CarEntity car = type.get().create(world, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        if (car == null) return InteractionResult.FAIL;
        float yaw = context.getRotation();
        car.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        car.setYHeadRot(yaw);
        car.setYBodyRot(yaw);
        car.initFresh(world.getRandom().nextInt(CarEntity.COLORS.length));
        if (context.getPlayer() != null) car.setOwner(context.getPlayer().getUUID());
        if (!world.noCollision(car)) return InteractionResult.FAIL;
        world.addFreshEntity(car);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
