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
    public static final Item SEDAN_KEY = register("sedan_key", p -> new CarKeyItem(p, () -> ModEntities.SEDAN), new Item.Properties().stacksTo(1));
    public static final Item SPORTS_KEY = register("sports_car_key", p -> new CarKeyItem(p, () -> ModEntities.SPORTS_CAR), new Item.Properties().stacksTo(1));
    public static final Item POLICE_KEY = register("police_car_key", p -> new CarKeyItem(p, () -> ModEntities.POLICE_CAR), new Item.Properties().stacksTo(1));
    public static final Item REPAIR_KIT = register("repair_kit", Item::new, new Item.Properties().stacksTo(16));
    public static final Item PACKAGE = register("package", Item::new, new Item.Properties().stacksTo(16));
    public static final Item CITY_KIT = register("city_kit", CityKitItem::new, new Item.Properties().stacksTo(16));

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
