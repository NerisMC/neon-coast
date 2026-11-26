package com.neoncoast.world;

import com.neoncoast.block.PropBlock;
import com.neoncoast.block.ShopTerminalBlock;
import com.neoncoast.registry.ModBlocks;
import com.neoncoast.registry.ModEntities;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;

/**
 * One 48x48 city district: a road grid on a 24-block pitch (neighbouring districts join into one big city),
 * crosswalks, traffic lights, hydrants, benches, street lights, and four lots with random buildings
 * (office towers, glass skyscrapers, shops, job centres, chop shops, police stations, parks).
 * Parked cars and citizens too.
 */
public final class CityGenerator {
    public static final int SIZE = 48;
    private static final int PITCH = 24;
    private static final int ROAD = 5;
    private static final int CLEAR_HEIGHT = 44;
    /** Shifts the road grid so districts (which start 16 blocks before a chunk that is a multiple of 3) line up with it. */
    public static final int OFF = 16;

    private enum Lot { TOWER, GLASS_TOWER, MIDRISE, SHOP, JOBS, CHOP_SHOP, POLICE, PARK }

    private CityGenerator() {
    }

    /** Snaps a position to the district grid (returns the north-west corner). */
    public static int snap(int v) {
        return Math.floorDiv(v + OFF, SIZE) * SIZE - OFF;
    }

    private static BlockState[] facades() {
        return new BlockState[]{
                ModBlocks.CONCRETE_PANEL.getDefaultState(), ModBlocks.DARK_PANEL.getDefaultState(),
                ModBlocks.MARBLE.getDefaultState(), ModBlocks.BRICK_FACADE.getDefaultState(),
                ModBlocks.METAL_CLADDING.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(),
                Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.LIGHT_BLUE_CONCRETE.getDefaultState()};
    }

    /** x0/z0 = north-west corner, y = street level (the road surface). */
    public static void generateDistrict(ServerWorldAccess world, int x0, int y, int z0, Random random) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        BlockState asphalt = ModBlocks.ASPHALT.getDefaultState();
        BlockState line = ModBlocks.ROAD_LINE.getDefaultState();
        BlockState walk = ModBlocks.SIDEWALK.getDefaultState();
        BlockState stone = Blocks.STONE.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        for (int dx = 0; dx < SIZE; dx++) {
            for (int dz = 0; dz < SIZE; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int lx = Math.floorMod(x + OFF, PITCH), lz = Math.floorMod(z + OFF, PITCH);
                boolean roadX = lx < ROAD, roadZ = lz < ROAD;
                BlockState surface;
                if (roadX || roadZ) {
                    boolean intersection = roadX && roadZ;
                    boolean centre = !intersection && ((roadX && lx == 2 && Math.floorMod(z, 4) < 2)
                            || (roadZ && lz == 2 && Math.floorMod(x, 4) < 2));
                    boolean crossing = !intersection && ((roadX && (lz == 6 || lz == PITCH - 2))
                            || (roadZ && (lx == 6 || lx == PITCH - 2)));
                    if (crossing) surface = ModBlocks.CROSSWALK.getDefaultState();
                    else if (centre) surface = line;
                    else if (Math.floorMod(x * 31 + z * 17, 97) == 0) surface = ModBlocks.MANHOLE.getDefaultState();
                    else surface = asphalt;
                } else if (lx == ROAD || lz == ROAD || lx == PITCH - 1 || lz == PITCH - 1) {
                    surface = walk;
                } else {
                    surface = Blocks.GRASS_BLOCK.getDefaultState();
                }
                for (int h = 1; h <= CLEAR_HEIGHT; h++) set(world, m.set(x, y + h, z), air);
                set(world, m.set(x, y, z), surface);
                for (int h = 1; h <= 8; h++) {
                    m.set(x, y - h, z);
                    BlockState below = world.getBlockState(m);
                    if (below.isReplaceable() || !below.getFluidState().isEmpty() || below.isOf(Blocks.GRASS_BLOCK)) set(world, m, stone);
                    else break;
                }
                if (!roadX && !roadZ) streetFurniture(world, x, y, z, lx, lz, m);
            }
        }

        for (int i = 0; i < SIZE; i += PITCH) {
            for (int j = 0; j < SIZE; j += PITCH) {
                int lotX = Math.floorDiv(x0 + i + OFF, PITCH) * PITCH - OFF + ROAD + 1;
                int lotZ = Math.floorDiv(z0 + j + OFF, PITCH) * PITCH - OFF + ROAD + 1;
                int lotSize = PITCH - ROAD - 2; // 17
                if (lotX < x0 || lotZ < z0 || lotX + lotSize > x0 + SIZE || lotZ + lotSize > z0 + SIZE) continue;
                buildLot(world, pickLot(random), lotX, y, lotZ, lotSize, random, m);
            }
        }

        spawnTraffic(world, x0, y, z0, random);
    }

    /** Lights, traffic lights, hydrants, benches and bins on the sidewalk ring of each block. */
    private static void streetFurniture(ServerWorldAccess world, int x, int y, int z, int lx, int lz, BlockPos.Mutable m) {
        if (lx == ROAD && lz == ROAD) {
            trafficLight(world, x, y, z, Direction.NORTH, m);
        } else if (lx == PITCH - 1 && lz == PITCH - 1) {
            trafficLight(world, x, y, z, Direction.SOUTH, m);
        } else if (lx == PITCH - 1 && lz == ROAD) {
            streetLight(world, x, y, z, m);
        } else if (lx == ROAD && lz == PITCH - 1) {
            streetLight(world, x, y, z, m);
        } else if (lx == ROAD && lz == 12) {
            set(world, m.set(x, y + 1, z), prop(ModBlocks.FIRE_HYDRANT, Direction.WEST));
        } else if (lz == ROAD && lx == 14) {
            set(world, m.set(x, y + 1, z), prop(ModBlocks.BENCH, Direction.NORTH));
        } else if (lz == ROAD && lx == 10) {
            set(world, m.set(x, y + 1, z), prop(ModBlocks.TRASH_CAN, Direction.NORTH));
        } else if (lz == PITCH - 1 && lx == 16) {
            set(world, m.set(x, y + 1, z), prop(ModBlocks.BENCH, Direction.SOUTH));
        }
    }

    private static Lot pickLot(Random random) {
        int r = random.nextInt(100);
        if (r < 20) return Lot.TOWER;
        if (r < 34) return Lot.GLASS_TOWER;
        if (r < 48) return Lot.MIDRISE;
        if (r < 62) return Lot.SHOP;
        if (r < 72) return Lot.JOBS;
        if (r < 80) return Lot.CHOP_SHOP;
        if (r < 87) return Lot.POLICE;
        return Lot.PARK;
    }

    private static void buildLot(ServerWorldAccess world, Lot type, int x0, int y, int z0, int size, Random random, BlockPos.Mutable m) {
        BlockState[] facades = facades();
        BlockState window = ModBlocks.OFFICE_WINDOW.getDefaultState();
        switch (type) {
            case TOWER -> {
                int h = 16 + random.nextInt(18);
                building(world, x0 + 1, y, z0 + 1, size - 2, size - 2, h, facades[random.nextInt(facades.length)], window, random, m);
                if (random.nextBoolean()) neonCrown(world, x0 + 1, y + h - 1, z0 + 1, size - 2, random, m);
            }
            case GLASS_TOWER -> {
                int h = 22 + random.nextInt(18);
                building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, h, ModBlocks.METAL_CLADDING.getDefaultState(),
                        ModBlocks.GLASS_CURTAIN_WALL.getDefaultState(), random, m);
                neonCrown(world, x0 + 2, y + h - 1, z0 + 2, size - 4, random, m);
            }
            case MIDRISE -> building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, 8 + random.nextInt(8),
                    facades[random.nextInt(facades.length)], window, random, m);
            case SHOP -> {
                building(world, x0 + 3, y, z0 + 3, size - 6, size - 6, 5, facades[random.nextInt(facades.length)],
                        Blocks.GLASS.getDefaultState(), random, m);
                awning(world, x0 + 3, y + 4, z0 + 2, size - 6, m);
                neonSign(world, x0 + 3, y + 5, z0 + 3, size - 6, random, m);
                set(world, m.set(x0 + size / 2, y + 1, z0 + size / 2),
                        ModBlocks.SHOP_TERMINAL.getDefaultState().with(ShopTerminalBlock.FACING, Direction.NORTH));
                set(world, m.set(x0 + 4, y + 1, z0 + 1), prop(ModBlocks.VENDING_MACHINE, Direction.NORTH));
                set(world, m.set(x0 + size - 5, y + 1, z0 + 1), prop(ModBlocks.TRASH_CAN, Direction.NORTH));
                set(world, m.set(x0 + size - 2, y + 1, z0 + size - 2), prop(ModBlocks.DUMPSTER, Direction.WEST));
            }
            case JOBS -> {
                building(world, x0 + 3, y, z0 + 3, size - 6, size - 6, 6, ModBlocks.BRICK_FACADE.getDefaultState(),
                        window, random, m);
                neonSign(world, x0 + 3, y + 5, z0 + 3, size - 6, random, m);
                set(world, m.set(x0 + size / 2, y + 1, z0 + size / 2), ModBlocks.JOB_BOARD.getDefaultState());
                set(world, m.set(x0 + 1, y + 1, z0 + size - 2), prop(ModBlocks.DUMPSTER, Direction.EAST));
            }
            case CHOP_SHOP -> {
                building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, 6, ModBlocks.METAL_CLADDING.getDefaultState(),
                        ModBlocks.DARK_PANEL.getDefaultState(), random, m);
                for (int dx = 2; dx < size - 2; dx++) {
                    for (int h = 1; h <= 4; h++) {
                        boolean opening = dx >= 5 && dx < size - 5;
                        set(world, m.set(x0 + dx, y + h, z0 + 2), opening ? Blocks.AIR.getDefaultState() : ModBlocks.GARAGE_DOOR.getDefaultState());
                    }
                }
                for (int dx = 3; dx < size - 3; dx++) {
                    for (int dz = 3; dz < size - 3; dz++) set(world, m.set(x0 + dx, y, z0 + dz), ModBlocks.ASPHALT.getDefaultState());
                }
                set(world, m.set(x0 + size / 2, y + 1, z0 + size - 4), ModBlocks.CHOP_SHOP.getDefaultState());
                set(world, m.set(x0 + 4, y + 1, z0 + size - 4), prop(ModBlocks.DUMPSTER, Direction.NORTH));
            }
            case POLICE -> {
                building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, 8, ModBlocks.CONCRETE_PANEL.getDefaultState(),
                        window, random, m);
                for (int dx = 3; dx < size - 3; dx++) set(world, m.set(x0 + dx, y + 7, z0 + 1), ModBlocks.NEON[1].getDefaultState());
                CarEntity cruiser = ModEntities.POLICE_CAR.create(world.toServerWorld());
                if (cruiser != null) {
                    cruiser.refreshPositionAndAngles(x0 + size / 2.0, y + 1, z0 - 1.5, 90.0f, 0.0f);
                    cruiser.initFresh(0);
                    world.spawnEntityAndPassengers(cruiser);
                }
            }
            case PARK -> {
                for (int dx = 0; dx < size; dx++) {
                    for (int dz = 0; dz < size; dz++) {
                        if (random.nextInt(9) == 0) set(world, m.set(x0 + dx, y + 1, z0 + dz), Blocks.SHORT_GRASS.getDefaultState());
                        else if (random.nextInt(25) == 0) set(world, m.set(x0 + dx, y + 1, z0 + dz), Blocks.POPPY.getDefaultState());
                    }
                }
                for (int t = 0; t < 3; t++) tree(world, x0 + 3 + random.nextInt(size - 6), y, z0 + 3 + random.nextInt(size - 6), m);
                set(world, m.set(x0 + size / 2, y + 1, z0 + size / 2), prop(ModBlocks.BENCH, Direction.SOUTH));
                set(world, m.set(x0 + size / 2 + 2, y + 1, z0 + size / 2), prop(ModBlocks.TRASH_CAN, Direction.SOUTH));
            }
        }
    }

    /** Hollow building: window bands, lit floors every 4 blocks, door on the north side, gravel roof with AC units. */
    private static void building(ServerWorldAccess world, int x0, int y, int z0, int w, int d, int h,
                                 BlockState wall, BlockState window, Random random, BlockPos.Mutable m) {
        BlockState floor = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState light = Blocks.SEA_LANTERN.getDefaultState();
        BlockState roof = ModBlocks.ROOF_GRAVEL.getDefaultState();
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                boolean edge = dx == 0 || dz == 0 || dx == w - 1 || dz == d - 1;
                boolean corner = (dx == 0 || dx == w - 1) && (dz == 0 || dz == d - 1);
                set(world, m.set(x0 + dx, y, z0 + dz), floor);
                for (int hh = 1; hh <= h; hh++) {
                    if (edge) {
                        boolean win = !corner && hh % 4 != 0 && hh % 4 != 1;
                        set(world, m.set(x0 + dx, y + hh, z0 + dz), win ? window : wall);
                    } else if (hh % 4 == 0 && hh < h) {
                        set(world, m.set(x0 + dx, y + hh, z0 + dz), (dx % 4 == 2 && dz % 4 == 2) ? light : floor);
                    }
                }
                set(world, m.set(x0 + dx, y + h + 1, z0 + dz), edge ? wall : roof);
            }
        }
        for (int dx = 2; dx < w - 2; dx += 4) set(world, m.set(x0 + dx, y + Math.min(h, 3) + 1, z0 + d / 2), light);
        int door = x0 + w / 2;
        for (int hh = 1; hh <= 3; hh++) {
            set(world, m.set(door, y + hh, z0), Blocks.AIR.getDefaultState());
            set(world, m.set(door - 1, y + hh, z0), Blocks.AIR.getDefaultState());
        }
        int acs = 1 + random.nextInt(3);
        for (int i = 0; i < acs; i++) {
            set(world, m.set(x0 + 2 + random.nextInt(Math.max(1, w - 4)), y + h + 2, z0 + 2 + random.nextInt(Math.max(1, d - 4))),
                    prop(ModBlocks.AC_UNIT, Direction.Type.HORIZONTAL.random(random)));
        }
    }

    private static void neonCrown(ServerWorldAccess world, int x0, int y, int z0, int w, Random random, BlockPos.Mutable m) {
        BlockState neon = ModBlocks.NEON[random.nextInt(ModBlocks.NEON.length)].getDefaultState();
        for (int i = 0; i < w; i++) {
            set(world, m.set(x0 + i, y, z0), neon);
            set(world, m.set(x0 + i, y, z0 + w - 1), neon);
            set(world, m.set(x0, y, z0 + i), neon);
            set(world, m.set(x0 + w - 1, y, z0 + i), neon);
        }
    }

    private static void neonSign(ServerWorldAccess world, int x0, int y, int z0, int w, Random random, BlockPos.Mutable m) {
        BlockState neon = ModBlocks.NEON[random.nextInt(ModBlocks.NEON.length)].getDefaultState();
        for (int dx = 2; dx < w - 2; dx++) set(world, m.set(x0 + dx, y, z0 - 1), neon);
    }

    private static void awning(ServerWorldAccess world, int x0, int y, int z, int w, BlockPos.Mutable m) {
        for (int dx = 0; dx < w; dx++) set(world, m.set(x0 + dx, y, z), ModBlocks.SHOP_AWNING.getDefaultState());
    }

    private static void spawnTraffic(ServerWorldAccess world, int x0, int y, int z0, Random random) {
        int cars = 2 + random.nextInt(3);
        for (int c = 0; c < cars; c++) {
            boolean alongX = random.nextBoolean();
            int along = 6 + random.nextInt(SIZE - 12);
            double x, z;
            float yaw;
            if (alongX) {
                x = x0 + along + 0.5;
                z = (Math.floorDiv(z0 + OFF, PITCH) * PITCH - OFF) + (random.nextBoolean() ? 0 : PITCH) + 1.0;
                yaw = random.nextBoolean() ? 90.0f : 270.0f;
            } else {
                x = (Math.floorDiv(x0 + OFF, PITCH) * PITCH - OFF) + (random.nextBoolean() ? 0 : PITCH) + 1.0;
                z = z0 + along + 0.5;
                yaw = random.nextBoolean() ? 0.0f : 180.0f;
            }
            if (x < x0 + 1 || x > x0 + SIZE - 2 || z < z0 + 1 || z > z0 + SIZE - 2) continue;
            EntityType<CarEntity> type = random.nextInt(4) == 0 ? ModEntities.SPORTS_CAR : ModEntities.SEDAN;
            CarEntity car = type.create(world.toServerWorld());
            if (car == null) continue;
            car.refreshPositionAndAngles(x, y + 1, z, yaw, 0.0f);
            car.setHeadYaw(yaw);
            car.setBodyYaw(yaw);
            car.initFresh(random.nextInt(CarEntity.COLORS.length));
            car.setCivilian(true);
            world.spawnEntityAndPassengers(car);
        }
        int people = 2 + random.nextInt(4);
        int blockX = Math.floorDiv(x0 + OFF, PITCH) * PITCH - OFF;
        int blockZ = Math.floorDiv(z0 + OFF, PITCH) * PITCH - OFF;
        for (int p = 0; p < people; p++) {
            VillagerEntity citizen = EntityType.VILLAGER.create(world.toServerWorld());
            if (citizen == null) continue;
            double px = blockX + PITCH * random.nextInt(2) + ROAD + 1 + random.nextInt(PITCH - ROAD - 2) + 0.5;
            double pz = blockZ + PITCH * random.nextInt(2) + ROAD + 0.5;
            if (px < x0 + 1 || px > x0 + SIZE - 1 || pz < z0 + 1 || pz > z0 + SIZE - 1) continue;
            citizen.refreshPositionAndAngles(px, y + 1, pz, random.nextFloat() * 360.0f, 0.0f);
            citizen.setPersistent();
            world.spawnEntityAndPassengers(citizen);
        }
    }

    private static BlockState prop(Block block, Direction facing) {
        return block.getDefaultState().with(PropBlock.FACING, facing);
    }

    private static void trafficLight(ServerWorldAccess world, int x, int y, int z, Direction facing, BlockPos.Mutable m) {
        for (int h = 1; h <= 3; h++) set(world, m.set(x, y + h, z), Blocks.IRON_BARS.getDefaultState());
        set(world, m.set(x, y + 4, z), prop(ModBlocks.TRAFFIC_LIGHT, facing));
    }

    private static void streetLight(ServerWorldAccess world, int x, int y, int z, BlockPos.Mutable m) {
        for (int h = 1; h <= 4; h++) set(world, m.set(x, y + h, z), Blocks.IRON_BARS.getDefaultState());
        set(world, m.set(x, y + 5, z), Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false));
    }

    private static void tree(ServerWorldAccess world, int x, int y, int z, BlockPos.Mutable m) {
        for (int h = 1; h <= 4; h++) set(world, m.set(x, y + h, z), Blocks.OAK_LOG.getDefaultState());
        BlockState leaves = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int h = 4; h <= 6; h++) {
                    if (Math.abs(dx) + Math.abs(dz) + (h - 4) <= 3 && !(dx == 0 && dz == 0 && h < 5)) {
                        set(world, m.set(x + dx, y + h, z + dz), leaves);
                    }
                }
            }
        }
    }

    private static void set(ServerWorldAccess world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }
}
