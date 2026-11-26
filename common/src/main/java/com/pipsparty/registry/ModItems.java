package com.pipsparty.registry;

import com.pipsparty.PipsParty;
import com.pipsparty.entity.Character;
import com.pipsparty.item.AnimatronicItem;
import com.pipsparty.item.CameraTabletItem;
import com.pipsparty.item.OverrideChipItem;
import com.pipsparty.item.PizzeriaKitItem;
import com.pipsparty.item.SecurityCameraItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Reg<Item> PIP_COIN = item("pip_coin", Item::new, new Item.Properties());
    public static final Reg<Item> CAMERA_TABLET = item("camera_tablet", CameraTabletItem::new, new Item.Properties().stacksTo(1));
    public static final Reg<Item> SECURITY_CAMERA = item("security_camera", SecurityCameraItem::new, new Item.Properties().stacksTo(16));
    public static final Reg<Item> OVERRIDE_CHIP = item("override_chip", OverrideChipItem::new, new Item.Properties().stacksTo(1));
    public static final Reg<Item> PIZZERIA_KIT = item("pizzeria_kit", PizzeriaKitItem::new, new Item.Properties().stacksTo(1));
    public static final Reg<Item> PIZZA_SLICE = item("pizza_slice", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.5f).build()));
    public static final Reg<Item> PARTY_SODA = item("party_soda", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build()));
    public static final Map<Character, Reg<Item>> ANIMATRONIC_ITEMS = new LinkedHashMap<>();

    static {
        for (Character c : Character.values()) {
            ANIMATRONIC_ITEMS.put(c, item(c.name().toLowerCase() + "_animatronic",
                    p -> new AnimatronicItem(p, () -> ModEntities.of(c).get()), new Item.Properties().stacksTo(1)));
        }
    }

    private ModItems() {
    }

    private static Reg<Item> item(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        return Reg.<Item>of(Registries.ITEM, PipsParty.id(name), key -> factory.apply(props.setId(key)));
    }

    public static void init() {
    }
}
