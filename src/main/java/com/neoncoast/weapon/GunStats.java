package com.neoncoast.weapon;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

/**
 * Everything that makes one gun feel different from another.
 *
 * @param damage     damage per pellet (a player has 20 health)
 * @param pellets    1 for bullets, more for shotgun buckshot
 * @param spread     random spread in radians (standard deviation)
 * @param fireDelay  ticks between shots
 * @param mag        rounds per magazine
 * @param range      blocks
 * @param reloadTicks how long a reload takes
 * @param automatic  hold right-click to keep firing
 * @param recoil     how far the camera kicks up per shot (degrees)
 */
public record GunStats(String id, float damage, int pellets, float spread, int fireDelay, int mag, double range,
                       int reloadTicks, boolean automatic, float recoil, Supplier<Item> ammo, Supplier<SoundEvent> sound) {
}
