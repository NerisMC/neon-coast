package com.neoncoast.crime;

import com.neoncoast.npc.PoliceOfficerEntity;
import com.neoncoast.registry.ModEntities;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;

/** Sends cops after wanted players: more stars, more cops, and police cars from 2 stars up. */
public final class PoliceSpawner {
    private PoliceSpawner() {
    }

    static void tick(ServerPlayer player, int stars) {
        ServerLevel world = player.level();
        if (stars <= 0 || player.isCreative() || player.isSpectator()) return;
        if (player.level().getServer().getTickCount() % 100 != 0) return; // every 5 seconds
        int officers = world.getEntitiesOfClass(PoliceOfficerEntity.class, player.getBoundingBox().inflate(64.0), e -> true).size();
        int cars = world.getEntitiesOfClass(CarEntity.class, player.getBoundingBox().inflate(96.0), CarEntity::isPolice).size();
        RandomSource r = world.getRandom();
        if (officers < Math.min(2 * stars, 10)) {
            BlockPos pos = spot(world, player, r, 18, 28);
            PoliceOfficerEntity cop = ModEntities.POLICE_OFFICER.create(world, EntitySpawnReason.EVENT);
            if (pos != null && cop != null) {
                cop.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360.0f, 0.0f);
                cop.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
                world.addFreshEntity(cop);
            }
        }
        if (stars >= 2 && cars < stars - 1) {
            BlockPos pos = spot(world, player, r, 40, 60);
            CarEntity car = ModEntities.POLICE_CAR.create(world, EntitySpawnReason.EVENT);
            PoliceOfficerEntity driver = ModEntities.POLICE_OFFICER.create(world, EntitySpawnReason.EVENT);
            if (pos != null && car != null && driver != null) {
                float yaw = (float) Math.toDegrees(Math.atan2(-(player.getX() - pos.getX()), player.getZ() - pos.getZ()));
                car.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
                car.initFresh(0);
                if (world.noCollision(car)) {
                    world.addFreshEntity(car);
                    driver.snapTo(car.getX(), car.getY(), car.getZ(), yaw, 0.0f);
                    driver.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
                    world.addFreshEntity(driver);
                    driver.startRiding(car, true, true);
                }
            }
        }
    }

    private static BlockPos spot(ServerLevel world, ServerPlayer player, RandomSource r, int min, int max) {
        for (int tries = 0; tries < 6; tries++) {
            float a = r.nextFloat() * Mth.TWO_PI;
            int d = min + r.nextInt(max - min);
            int x = Mth.floor(player.getX() + Mth.cos(a) * d);
            int z = Mth.floor(player.getZ() + Mth.sin(a) * d);
            if (!world.hasChunk(x >> 4, z >> 4)) continue;
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (world.getFluidState(pos.below()).isEmpty() && Math.abs(y - player.getY()) < 12) return pos;
        }
        return null;
    }
}
