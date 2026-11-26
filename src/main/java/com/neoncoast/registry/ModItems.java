package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import com.neoncoast.item.CarKeyItem;
import com.neoncoast.item.CityKitItem;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import com.neoncoast.weapon.GunItem;
import com.neoncoast.weapon.GunStats;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Item SEDAN_KEY = register("sedan_key", p -> new CarKeyItem(p, () -> ModEntities.SEDAN), new Item.Properties().stacksTo(1));
    public static final Item SPORTS_KEY = register("sports_car_key", p -> new CarKeyItem(p, () -> ModEntities.SPORTS_CAR), new Item.Properties().stacksTo(1));
    public static final Item POLICE_KEY = register("police_car_key", p -> new CarKeyItem(p, () -> ModEntities.POLICE_CAR), new Item.Properties().stacksTo(1));
    public static final Item REPAIR_KIT = register("repair_kit", Item::new, new Item.Properties().stacksTo(16));
    public static final Item PACKAGE = register("package", Item::new, new Item.Properties().stacksTo(16));
    public static final Item CITY_KIT = register("city_kit", CityKitItem::new, new Item.Properties().stacksTo(16));

    // ---- weapons
    public static final Item PISTOL_AMMO = register("pistol_ammo", Item::new, new Item.Properties());
    public static final Item RIFLE_AMMO = register("rifle_ammo", Item::new, new Item.Properties());
    public static final Item SHOTGUN_SHELLS = register("shotgun_shells", Item::new, new Item.Properties());
    public static final GunItem PISTOL = gun(new GunStats("pistol", 6.0f, 1, 0.012f, 5, 12, 64, 30, false, 2.5f,
            () -> PISTOL_AMMO, () -> ModSounds.GUN_PISTOL));
    public static final GunItem SMG = gun(new GunStats("smg", 4.0f, 1, 0.035f, 2, 30, 48, 40, true, 1.2f,
            () -> PISTOL_AMMO, () -> ModSounds.GUN_SMG));
    public static final GunItem SHOTGUN = gun(new GunStats("shotgun", 3.5f, 8, 0.075f, 18, 6, 32, 60, false, 6.0f,
            () -> SHOTGUN_SHELLS, () -> ModSounds.GUN_SHOTGUN));
    public static final GunItem RIFLE = gun(new GunStats("rifle", 7.0f, 1, 0.016f, 3, 30, 96, 50, true, 1.6f,
            () -> RIFLE_AMMO, () -> ModSounds.GUN_RIFLE));
    public static final GunItem SNIPER = gun(new GunStats("sniper", 26.0f, 1, 0.0f, 30, 5, 160, 60, false, 8.0f,
            () -> RIFLE_AMMO, () -> ModSounds.GUN_SNIPER));

    private static GunItem gun(GunStats stats) {
        return register(stats.id(), p -> new GunItem(p, stats), new Item.Properties());
    }

    private ModItems() {
    }

    public static Item keyFor(EntityType<?> type) {
        if (type == ModEntities.SPORTS_CAR) return SPORTS_KEY;
        if (type == ModEntities.POLICE_CAR) return POLICE_KEY;
        return SEDAN_KEY;
    }

    public static <T extends Item> T register(String name, java.util.function.Function<Item.Properties, T> factory, Item.Properties props) {
        net.minecraft.resources.ResourceKey<Item> key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, NeonCoast.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
    }

    public static void init() {
    }
}
