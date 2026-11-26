package com.pipsparty.item;

import com.pipsparty.world.PizzeriaGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Builds a complete Pip's Party Palace (40 x 34) with its south-west corner at the clicked block. */
public class PizzeriaKitItem extends Item {
    public PizzeriaKitItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        PizzeriaGenerator.generate(level, ctx.getClickedPos().north(PizzeriaGenerator.DEPTH - 1), level.getRandom());
        if (ctx.getPlayer() != null) {
            ctx.getPlayer().sendSystemMessage(Component.literal("Your Pip's Party Palace is built! The office is in the north-east corner. "
                    + "Clock in at the Time Clock when you're ready for your shift...").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        ctx.getItemInHand().consume(1, ctx.getPlayer());
        return InteractionResult.CONSUME;
    }
}
