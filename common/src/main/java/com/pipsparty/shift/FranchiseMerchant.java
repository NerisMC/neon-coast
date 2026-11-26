package com.pipsparty.shift;

import com.pipsparty.entity.Character;
import com.pipsparty.registry.ModBlocks;
import com.pipsparty.registry.ModItems;
import com.pipsparty.registry.Reg;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Block;

/** The Franchise Supply Catalog's trade list: Pip Coins for everything you need to run (and survive) a pizzeria. */
public class FranchiseMerchant implements Merchant {
    private final ServerPlayer player;
    private MerchantOffers offers;

    public FranchiseMerchant(ServerPlayer player) {
        this.player = player;
    }

    private static MerchantOffer offer(int coins, Item item, int count) {
        return new MerchantOffer(new ItemCost(ModItems.PIP_COIN.get(), coins), new ItemStack(item, count), 99999, 0, 0.0f);
    }

    private static MerchantOffer offer(int coins, Reg<Block> block, int count) {
        return offer(coins, block.get().asItem(), count);
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            offers = new MerchantOffers();
            offers.add(offer(4, ModItems.CAMERA_TABLET.get(), 1));
            offers.add(offer(3, ModItems.SECURITY_CAMERA.get(), 1));
            offers.add(offer(6, ModBlocks.SECURITY_DOOR, 2));
            offers.add(offer(2, ModBlocks.DOOR_BUTTON, 1));
            offers.add(offer(2, ModBlocks.LIGHT_BUTTON, 1));
            offers.add(offer(2, ModBlocks.HALL_LIGHT, 1));
            offers.add(offer(5, ModBlocks.TIME_CLOCK, 1));
            offers.add(offer(12, ModBlocks.ARCADE_CABINET, 1));
            offers.add(offer(9, ModBlocks.PRIZE_COUNTER, 1));
            offers.add(offer(6, ModBlocks.TICKET_MACHINE, 1));
            offers.add(offer(4, ModBlocks.PARTY_TABLE, 2));
            offers.add(offer(2, ModBlocks.PARTY_CHAIR, 4));
            offers.add(offer(3, ModBlocks.BALL_PIT, 8));
            offers.add(offer(8, ModBlocks.PIZZA_OVEN, 1));
            offers.add(offer(2, ModBlocks.PARTY_BALLOONS, 2));
            offers.add(offer(1, ModBlocks.CHECKER_FLOOR, 16));
            offers.add(offer(1, ModBlocks.PARTY_WALL_RED, 16));
            offers.add(offer(1, ModBlocks.STAGE_FLOOR, 8));
            offers.add(offer(2, ModBlocks.STAGE_CURTAIN, 8));
            offers.add(offer(2, ModBlocks.POSTER_PIP, 2));
            offers.add(offer(1, ModItems.PIZZA_SLICE.get(), 4));
            offers.add(offer(1, ModItems.PARTY_SODA.get(), 3));
            offers.add(offer(20, ModItems.OVERRIDE_CHIP.get(), 1));
            int price = 40;
            for (Character c : Character.values()) {
                offers.add(offer(price, ModItems.ANIMATRONIC_ITEMS.get(c).get(), 1));
                price += 10;
            }
            offers.add(offer(64, ModItems.PIZZERIA_KIT.get(), 1));
        }
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
    }

    @Override
    public void setTradingPlayer(Player player) {
    }

    @Override
    public Player getTradingPlayer() {
        return player;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.EXPERIENCE_ORB_PICKUP;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public boolean stillValid(Player p) {
        return p == player && p.isAlive();
    }
}
