package com.neoncoast.economy;

import com.neoncoast.crime.CrimeHooks;
import com.neoncoast.crime.WantedManager;
import com.neoncoast.registry.ModBlocks;
import com.neoncoast.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ShopLogic {
    private static final Map<BlockPos, Long> LAST_ROBBED = new HashMap<>();

    private ShopLogic() {
    }

    public static void buy(ServerPlayer player, int index, BlockPos shop) {
        if (index < 0 || index >= ShopCatalog.NAMES.length) return;
        if (player.distanceToSqr(shop.getCenter()) > 64.0 || !player.level().getBlockState(shop).is(ModBlocks.SHOP_TERMINAL)) return;

        if (index == ShopCatalog.ROB) {
            long now = player.level().getGameTime();
            Long last = LAST_ROBBED.get(shop);
            if (last != null && now - last < 6000) {
                say(player, "The register is empty. Come back later.", ChatFormatting.GRAY);
                return;
            }
            LAST_ROBBED.put(shop.immutable(), now);
            long take = 300 + player.getRandom().nextInt(500);
            Money.add(player, take);
            player.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.2f);
            CrimeHooks.crime(player, 2, "Armed robbery (+$" + take + ")");
            return;
        }
        if (index == ShopCatalog.PAY_OFF && WantedManager.getStars(player) == 0) {
            say(player, "Nobody's looking for you.", ChatFormatting.GRAY);
            return;
        }
        long price = ShopCatalog.PRICES[index];
        if (!Money.spend(player, price)) {
            say(player, "Not enough cash ($" + price + ")", ChatFormatting.RED);
            return;
        }
        switch (index) {
            case 0 -> give(player, new ItemStack(ModItems.SEDAN_KEY));
            case 1 -> give(player, new ItemStack(ModItems.SPORTS_KEY));
            case 2 -> {
                ItemStack armor = new ItemStack(Items.IRON_CHESTPLATE);
                armor.set(DataComponents.CUSTOM_NAME, Component.literal("Body Armor").withStyle(ChatFormatting.AQUA));
                give(player, armor);
            }
            case 3 -> give(player, new ItemStack(ModItems.REPAIR_KIT));
            case 4 -> give(player, new ItemStack(Items.COOKED_BEEF, 8));
            case 5 -> give(player, new ItemStack(ModItems.PISTOL));
            case 6 -> give(player, new ItemStack(ModItems.SMG));
            case 7 -> give(player, new ItemStack(ModItems.SHOTGUN));
            case 8 -> give(player, new ItemStack(ModItems.RIFLE));
            case 9 -> give(player, new ItemStack(ModItems.SNIPER));
            case 10 -> give(player, new ItemStack(ModItems.PISTOL_AMMO, 30));
            case 11 -> give(player, new ItemStack(ModItems.RIFLE_AMMO, 30));
            case 12 -> give(player, new ItemStack(ModItems.SHOTGUN_SHELLS, 12));
            case ShopCatalog.PAY_OFF -> WantedManager.clear(player);
            default -> { }
        }
        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        say(player, "Bought " + ShopCatalog.NAMES[index] + " for $" + price, ChatFormatting.GREEN);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static void say(ServerPlayer player, String msg, ChatFormatting color) {
        player.sendOverlayMessage(Component.literal(msg).withStyle(color));
    }
}
