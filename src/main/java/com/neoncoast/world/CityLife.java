package com.neoncoast.world;

import com.neoncoast.npc.CitizenEntity;
import com.neoncoast.registry.ModBlocks;
import com.neoncoast.registry.ModEntities;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Keeps the streets alive around players who are in a city: pedestrians on the sidewalks and
 * citizens driving cars on the roads. They despawn again when nobody is near.
 */
public final class CityLife {
    private static final int MAX_PEDESTRIANS = 14;
    private static final int MAX_TRAFFIC = 6;

    private CityLife() {
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) continue;
            ServerLevel level = player.level();
            if (!inCity(level, player.blockPosition())) continue;
            RandomSource r = level.getRandom();
            int peds = level.getEntitiesOfClass(CitizenEntity.class, player.getBoundingBox().inflate(48.0), c -> !c.isPassenger()).size();
            if (peds < MAX_PEDESTRIANS) spawnPedestrian(level, player, r);
            int cars = level.getEntitiesOfClass(CarEntity.class, player.getBoundingBox().inflate(72.0), CarEntity::isNpcDriven).size();
            if (cars < MAX_TRAFFIC) spawnTraffic(level, player, r);
        }
    }

    private static boolean inCity(ServerLevel level, BlockPos at) {
        int hits = 0;
        for (int dx = -16; dx <= 16; dx += 8) {
            for (int dz = -16; dz <= 16; dz += 8) {
                BlockState s = surface(level, at.getX() + dx, at.getZ() + dz);
                if (s != null && (CarEntity.isRoad(s) || s.is(ModBlocks.SIDEWALK))) hits++;
            }
        }
        return hits >= 4;
    }

    private static BlockState surface(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return level.getBlockState(new BlockPos(x, y - 1, z));
    }

    private static BlockPos spot(ServerLevel level, ServerPlayer player, RandomSource r, int min, int max, boolean road) {
        for (int tries = 0; tries < 10; tries++) {
            float a = r.nextFloat() * Mth.TWO_PI;
            int d = min + r.nextInt(max - min);
            int x = Mth.floor(player.getX() + Mth.cos(a) * d), z = Mth.floor(player.getZ() + Mth.sin(a) * d);
            BlockState s = surface(level, x, z);
            if (s == null) continue;
            boolean ok = road ? CarEntity.isRoad(s) : (s.is(ModBlocks.SIDEWALK) || s.is(ModBlocks.CROSSWALK));
            if (!ok) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - player.getY()) > 16) continue;
            if (level.getNearestPlayer(x + 0.5, y, z + 0.5, min * 0.7, false) != null) continue;
            return new BlockPos(x, y, z);
        }
        return null;
    }

    private static void spawnPedestrian(ServerLevel level, ServerPlayer player, RandomSource r) {
        BlockPos pos = spot(level, player, r, 20, 44, false);
        if (pos == null) return;
        CitizenEntity c = ModEntities.CITIZEN.create(level, EntitySpawnReason.NATURAL);
        if (c == null) return;
        c.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360.0f, 0.0f);
        c.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        c.setAmbient(true);
        if (level.noCollision(c)) level.addFreshEntity(c);
    }

    private static void spawnTraffic(ServerLevel level, ServerPlayer player, RandomSource r) {
        BlockPos pos = spot(level, player, r, 30, 60, true);
        if (pos == null) return;
        // face along the road: whichever axis has more road
        int alongX = 0, alongZ = 0;
        for (int i = -4; i <= 4; i++) {
            BlockState sx = surface(level, pos.getX() + i, pos.getZ());
            BlockState sz = surface(level, pos.getX(), pos.getZ() + i);
            if (sx != null && CarEntity.isRoad(sx)) alongX++;
            if (sz != null && CarEntity.isRoad(sz)) alongZ++;
        }
        float yaw = alongX > alongZ ? (r.nextBoolean() ? 90.0f : 270.0f) : (r.nextBoolean() ? 0.0f : 180.0f);
        EntityType<CarEntity> type = r.nextInt(4) == 0 ? ModEntities.SPORTS_CAR : ModEntities.SEDAN;
        CarEntity car = type.create(level, EntitySpawnReason.NATURAL);
        CitizenEntity driver = ModEntities.CITIZEN.create(level, EntitySpawnReason.NATURAL);
        if (car == null || driver == null) return;
        car.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        car.setYHeadRot(yaw);
        car.setYBodyRot(yaw);
        car.initFresh(r.nextInt(CarEntity.COLORS.length));
        car.setCivilian(true);
        car.setAmbientTraffic(true);
        if (!level.noCollision(car)) return;
        level.addFreshEntity(car);
        driver.snapTo(car.getX(), car.getY(), car.getZ(), yaw, 0.0f);
        driver.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        driver.setAmbient(true);
        level.addFreshEntity(driver);
        driver.startRiding(car, true, true);
    }
}
