package com.neoncoast.weapon;

import com.neoncoast.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import java.util.function.Consumer;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A hitscan gun. The magazine is stored as item damage (the bar under the icon is your ammo):
 * right-click to shoot (hold for automatics), sneak + right-click to reload. Reloading takes ammo from your inventory.
 */
public class GunItem extends Item {
    private final GunStats stats;

    public GunItem(Properties settings, GunStats stats) {
        super(settings.durability(stats.mag()));
        this.stats = stats;
    }

    public GunStats stats() {
        return stats;
    }

    public static int rounds(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) reload(player, stack);
            return InteractionResult.CONSUME;
        }
        if (stats.automatic()) player.startUsingItem(hand);
        tryFire(level, player, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (stats.automatic() && user instanceof Player player) tryFire(level, player, stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return rounds(stack) <= stats.mag() / 4 ? 0xFF5050 : 0xFFD24A;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.literal("Ammo: " + rounds(stack) + "/" + stats.mag()).withStyle(ChatFormatting.GOLD));
        lines.accept(Component.literal(stats.automatic() ? "Hold right-click: fire" : "Right-click: fire").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("Sneak + right-click: reload").withStyle(ChatFormatting.GRAY));
    }

    private void tryFire(Level level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) return;
        if (rounds(stack) <= 0) {
            player.getCooldowns().addCooldown(stack, 8);
            if (!level.isClientSide() && !reload(player, stack)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GUN_EMPTY, SoundSource.PLAYERS, 0.7f, 1.0f);
            }
            return;
        }
        player.getCooldowns().addCooldown(stack, stats.fireDelay());
        if (level.isClientSide()) {
            // recoil: the camera kicks up and a little sideways (the client owns the camera)
            player.setXRot(Mth.clamp(player.getXRot() - stats.recoil(), -90.0f, 90.0f));
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * stats.recoil() * 0.4f);
            return;
        }
        stack.setDamageValue(stack.getDamageValue() + 1);
        if (level instanceof ServerLevel server) {
            Ballistics.fire(server, player, stats, player.getViewVector(1.0f), 0.0f);
        }
    }

    /** Fills the magazine from ammo in the inventory. Returns false if there was nothing to load. */
    public boolean reload(Player player, ItemStack stack) {
        int need = stack.getDamageValue();
        if (need <= 0) return false;
        int loaded;
        if (player.hasInfiniteMaterials()) {
            loaded = need;
        } else {
            loaded = 0;
            Inventory inv = player.getInventory();
            Item ammo = stats.ammo().get();
            for (int i = 0; i < inv.getContainerSize() && loaded < need; i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(ammo)) {
                    int take = Math.min(s.getCount(), need - loaded);
                    s.shrink(take);
                    loaded += take;
                }
            }
        }
        if (loaded == 0) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.literal("Out of ammo! Buy more at a store.").withStyle(ChatFormatting.RED));
            }
            return false;
        }
        stack.setDamageValue(need - loaded);
        player.getCooldowns().addCooldown(stack, stats.reloadTicks());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GUN_RELOAD, SoundSource.PLAYERS, 0.8f, 1.0f);
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.literal("Reloading... " + rounds(stack) + "/" + stats.mag()).withStyle(ChatFormatting.GRAY));
        }
        return true;
    }
}
