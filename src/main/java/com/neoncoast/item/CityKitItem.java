package com.neoncoast.item;

import com.neoncoast.world.CityGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Builds a 2x2 block of city districts (96x96) with its corner where you click. */
public class CityKitItem extends Item {
    public CityKitItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel world)) return InteractionResult.SUCCESS;
        BlockPos at = context.getClickedPos();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                CityGenerator.generateDistrict(world, CityGenerator.snap(at.getX()) + i * CityGenerator.SIZE, at.getY(), CityGenerator.snap(at.getZ()) + j * CityGenerator.SIZE, world.getRandom());
            }
        }
        if (context.getPlayer() != null) {
            context.getPlayer().sendOverlayMessage(Component.literal("City built!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        }
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
