package com.neoncoast.block;

import com.neoncoast.crime.CrimeHooks;
import com.neoncoast.economy.Money;
import com.neoncoast.vehicle.CarEntity;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Park a car next to it and right-click: they strip it for parts and pay you. No questions asked. */
public class ChopShopBlock extends Block {
    public ChopShopBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        List<CarEntity> cars = world.getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(6.0), c -> !c.isVehicle());
        if (cars.isEmpty()) {
            sp.sendOverlayMessage(Component.literal("Park a car right next to the chop shop first.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        cars.sort(Comparator.comparingDouble(c -> c.distanceToSqr(pos.getCenter())));
        CarEntity car = cars.get(0);
        long value = car.isPolice() ? 1500 : car.getCarType().chopValue();
        value = Math.round(value * (0.4 + 0.6 * car.getCarHealth() / car.getCarType().maxHealth()));
        if (car.isPolice()) CrimeHooks.crime(sp, 1, "Chopped a police car");
        car.discard();
        Money.add(sp, value);
        world.playSound(null, pos, SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 0.7f);
        sp.sendOverlayMessage(Component.literal("Chopped for parts: +$" + value).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        return InteractionResult.SUCCESS;
    }
}
