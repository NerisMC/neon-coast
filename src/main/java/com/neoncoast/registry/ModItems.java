package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import com.neoncoast.item.CarKeyItem;
import com.neoncoast.item.CityKitItem;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Item SEDAN_KEY = register("sedan_key", new CarKeyItem(new Item.Properties().stacksTo(1), () -> ModEntities.SEDAN));
    public static final Item SPORTS_KEY = register("sports_car_key", new CarKeyItem(new Item.Properties().stacksTo(1), () -> ModEntities.SPORTS_CAR));
    public static final Item POLICE_KEY = register("police_car_key", new CarKeyItem(new Item.Properties().stacksTo(1), () -> ModEntities.POLICE_CAR));
    public static final Item REPAIR_KIT = register("repair_kit", new Item(new Item.Properties().stacksTo(16)));
    public static final Item PACKAGE = register("package", new Item(new Item.Properties().stacksTo(16)));
    public static final Item CITY_KIT = register("city_kit", new CityKitItem(new Item.Properties().stacksTo(16)));

    private ModItems() {
    }

    public static Item keyFor(EntityType<?> type) {
        if (type == ModEntities.SPORTS_CAR) return SPORTS_KEY;
        if (type == ModEntities.POLICE_CAR) return POLICE_KEY;
        return SEDAN_KEY;
    }

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, NeonCoast.id(name), item);
    }

    public static void init() {
    }
}
