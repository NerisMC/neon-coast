package com.neoncoast.world;

import com.neoncoast.block.PropBlock;
import com.neoncoast.block.ShopTerminalBlock;
import com.neoncoast.registry.ModBlocks;
import com.neoncoast.registry.ModEntities;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import com.neoncoast.npc.CitizenEntity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.BlockState;

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
    private static final int CLEAR_HEIGHT = 52;
    /** Shifts the road grid so districts (which start 16 blocks before a chunk that is a multiple of 3) line up with it. */
    public static final int OFF = 16;

    private enum Lot { OFFICE, GLASS_TOWER, APARTMENTS, SHOP, JOBS, CHOP_SHOP, POLICE, GAS_STATION, PARKING, PARK }

    private CityGenerator() {
    }

    /** Snaps a position to the district grid (returns the north-west corner). */
    public static int snap(int v) {
        return Math.floorDiv(v + OFF, SIZE) * SIZE - OFF;
    }

    private static BlockState[] facades() {
        return new BlockState[]{
                ModBlocks.CONCRETE_PANEL.defaultBlockState(), ModBlocks.DARK_PANEL.defaultBlockState(),
                ModBlocks.MARBLE.defaultBlockState(), ModBlocks.BRICK_FACADE.defaultBlockState(),
                ModBlocks.METAL_CLADDING.defaultBlockState(), Blocks.WHITE_CONCRETE.defaultBlockState(),
                Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()};
    }

    /** x0/z0 = north-west corner, y = street level (the road surface). */
    public static void generateDistrict(ServerLevelAccessor world, int x0, int y, int z0, RandomSource random) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState asphalt = ModBlocks.ASPHALT.defaultBlockState();
        BlockState line = ModBlocks.ROAD_LINE.defaultBlockState();
        BlockState walk = ModBlocks.SIDEWALK.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

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
                    if (crossing) surface = ModBlocks.CROSSWALK.defaultBlockState();
                    else if (centre) surface = line;
                    else if (Math.floorMod(x * 31 + z * 17, 97) == 0) surface = ModBlocks.MANHOLE.defaultBlockState();
                    else surface = asphalt;
                } else if (lx == ROAD || lz == ROAD || lx == PITCH - 1 || lz == PITCH - 1) {
                    surface = walk;
                } else {
                    surface = Blocks.GRASS_BLOCK.defaultBlockState();
                }
                for (int h = 1; h <= CLEAR_HEIGHT; h++) set(world, m.set(x, y + h, z), air);
                set(world, m.set(x, y, z), surface);
                for (int h = 1; h <= 8; h++) {
                    m.set(x, y - h, z);
                    BlockState below = world.getBlockState(m);
                    if (below.canBeReplaced() || !below.getFluidState().isEmpty() || below.is(Blocks.GRASS_BLOCK)) set(world, m, stone);
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
    private static void streetFurniture(ServerLevelAccessor world, int x, int y, int z, int lx, int lz, BlockPos.MutableBlockPos m) {
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

    private static Lot pickLot(RandomSource random) {
        int r = random.nextInt(100);
        if (r < 18) return Lot.OFFICE;
        if (r < 31) return Lot.GLASS_TOWER;
        if (r < 47) return Lot.APARTMENTS;
        if (r < 61) return Lot.SHOP;
        if (r < 68) return Lot.JOBS;
        if (r < 73) return Lot.CHOP_SHOP;
        if (r < 79) return Lot.POLICE;
        if (r < 86) return Lot.GAS_STATION;
        if (r < 92) return Lot.PARKING;
        return Lot.PARK;
    }

    /** A facade: main wall, horizontal floor bands, vertical pilasters, upper windows, ground floor glass, bay width. */
    private record Style(BlockState wall, BlockState trim, BlockState column, BlockState window, BlockState lobby, int bay) {
    }

    private static BlockState b(Block block) {
        return block.defaultBlockState();
    }

    private static Style officeStyle(RandomSource r) {
        BlockState win = b(ModBlocks.OFFICE_WINDOW);
        Style[] all = {
                new Style(b(ModBlocks.CONCRETE_PANEL), b(Blocks.SMOOTH_STONE), b(Blocks.LIGHT_GRAY_CONCRETE), win, b(Blocks.GLASS), 3),
                new Style(b(ModBlocks.MARBLE), b(Blocks.SMOOTH_QUARTZ), b(Blocks.QUARTZ_PILLAR), win, b(Blocks.GLASS), 3),
                new Style(b(ModBlocks.DARK_PANEL), b(Blocks.POLISHED_BLACKSTONE), b(ModBlocks.DARK_PANEL), b(Blocks.GRAY_STAINED_GLASS), b(Blocks.GLASS), 3),
                new Style(b(ModBlocks.BRICK_FACADE), b(Blocks.STONE_BRICKS), b(ModBlocks.BRICK_FACADE), win, b(Blocks.GLASS), 3),
                new Style(b(Blocks.WHITE_CONCRETE), b(Blocks.LIGHT_GRAY_CONCRETE), b(Blocks.WHITE_CONCRETE), b(Blocks.LIGHT_BLUE_STAINED_GLASS), b(Blocks.GLASS), 3),
                new Style(b(Blocks.DEEPSLATE_TILES), b(Blocks.POLISHED_ANDESITE), b(Blocks.DEEPSLATE_TILES), b(Blocks.BLACK_STAINED_GLASS), b(Blocks.GLASS), 3),
                new Style(b(Blocks.SMOOTH_SANDSTONE), b(Blocks.CUT_SANDSTONE), b(Blocks.SMOOTH_SANDSTONE), win, b(Blocks.GLASS), 3)};
        return all[r.nextInt(all.length)];
    }

    private static Style apartmentStyle(RandomSource r) {
        BlockState win = b(Blocks.GLASS);
        Style[] all = {
                new Style(b(Blocks.BRICKS), b(Blocks.SMOOTH_QUARTZ), b(Blocks.BRICKS), win, win, 4),
                new Style(b(Blocks.RED_TERRACOTTA), b(Blocks.WHITE_TERRACOTTA), b(Blocks.RED_TERRACOTTA), win, win, 4),
                new Style(b(Blocks.BROWN_TERRACOTTA), b(Blocks.SMOOTH_STONE), b(Blocks.BROWN_TERRACOTTA), win, win, 4),
                new Style(b(Blocks.MUD_BRICKS), b(Blocks.STONE_BRICKS), b(Blocks.MUD_BRICKS), win, win, 4),
                new Style(b(Blocks.WHITE_TERRACOTTA), b(Blocks.LIGHT_GRAY_TERRACOTTA), b(Blocks.WHITE_TERRACOTTA), win, win, 4),
                new Style(b(ModBlocks.BRICK_FACADE), b(Blocks.SMOOTH_QUARTZ), b(ModBlocks.BRICK_FACADE), win, win, 4)};
        return all[r.nextInt(all.length)];
    }

    private static void buildLot(ServerLevelAccessor world, Lot type, int x0, int y, int z0, int size, RandomSource random, BlockPos.MutableBlockPos m) {
        switch (type) {
            case OFFICE -> {
                int h = 4 * (4 + random.nextInt(5));      // 16-32
                building(world, x0 + 1, y, z0 + 1, size - 2, size - 2, h, officeStyle(random), true, Roof.OFFICE, random, m);
            }
            case GLASS_TOWER -> {
                int h = 4 * (6 + random.nextInt(4));      // 24-36
                BlockState glass = random.nextBoolean() ? b(ModBlocks.GLASS_CURTAIN_WALL) : b(Blocks.LIGHT_BLUE_STAINED_GLASS);
                Style st = new Style(glass, b(ModBlocks.METAL_CLADDING), b(ModBlocks.METAL_CLADDING), glass, b(Blocks.GLASS), 3);
                building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, h, st, true, Roof.HELIPAD, random, m);
            }
            case APARTMENTS -> {
                int h = 4 * (3 + random.nextInt(4));      // 12-24
                Style st = apartmentStyle(random);
                building(world, x0 + 1, y, z0 + 2, size - 2, size - 3, h, st, false, Roof.WATER_TANK, random, m);
                balconies(world, x0 + 1, y, z0 + 2, size - 2, h, st, random, m);
            }
            case SHOP -> shop(world, x0, y, z0, size, random, m);
            case JOBS -> {
                Style st = new Style(b(ModBlocks.BRICK_FACADE), b(Blocks.STONE_BRICKS), b(Blocks.STONE_BRICKS), b(ModBlocks.OFFICE_WINDOW), b(Blocks.GLASS), 3);
                building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, 8, st, false, Roof.PLAIN, random, m);
                neonSign(world, x0 + 2, y + 5, z0 + 2, size - 4, random, m);
                set(world, m.set(x0 + size / 2 + 2, y + 1, z0 + size / 2), ModBlocks.JOB_BOARD.defaultBlockState());
                set(world, m.set(x0 + 1, y + 1, z0 + size - 2), prop(ModBlocks.DUMPSTER, Direction.EAST));
            }
            case CHOP_SHOP -> {
                Style st = new Style(b(ModBlocks.METAL_CLADDING), b(ModBlocks.DARK_PANEL), b(ModBlocks.DARK_PANEL), b(ModBlocks.DARK_PANEL), b(ModBlocks.METAL_CLADDING), 4);
                building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, 6, st, false, Roof.PLAIN, random, m);
                for (int dx = 2; dx < size - 2; dx++) {
                    for (int h = 1; h <= 4; h++) {
                        boolean opening = dx >= 5 && dx < size - 5;
                        set(world, m.set(x0 + dx, y + h, z0 + 2), opening ? Blocks.AIR.defaultBlockState() : ModBlocks.GARAGE_DOOR.defaultBlockState());
                    }
                }
                for (int dx = 3; dx < size - 3; dx++) {
                    for (int dz = 3; dz < size - 3; dz++) set(world, m.set(x0 + dx, y, z0 + dz), ModBlocks.ASPHALT.defaultBlockState());
                }
                set(world, m.set(x0 + size / 2, y + 1, z0 + size - 4), ModBlocks.CHOP_SHOP.defaultBlockState());
                set(world, m.set(x0 + 4, y + 1, z0 + size - 4), prop(ModBlocks.DUMPSTER, Direction.NORTH));
            }
            case POLICE -> {
                Style st = new Style(b(Blocks.WHITE_CONCRETE), b(Blocks.BLUE_CONCRETE), b(Blocks.LIGHT_GRAY_CONCRETE), b(ModBlocks.OFFICE_WINDOW), b(Blocks.GLASS), 3);
                building(world, x0 + 2, y, z0 + 3, size - 4, size - 5, 12, st, false, Roof.OFFICE, random, m);
                for (int dx = 4; dx < size - 4; dx++) set(world, m.set(x0 + dx, y + 5, z0 + 2), ModBlocks.NEON[1].defaultBlockState());
                flagpole(world, x0 + 1, y, z0 + 1, m);
                CarEntity cruiser = ModEntities.POLICE_CAR.create(world.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
                if (cruiser != null) {
                    cruiser.snapTo(x0 + size / 2.0, y + 1, z0 - 1.5, 90.0f, 0.0f);
                    cruiser.initFresh(0);
                    world.addFreshEntityWithPassengers(cruiser);
                }
            }
            case GAS_STATION -> gasStation(world, x0, y, z0, size, random, m);
            case PARKING -> parking(world, x0, y, z0, size, random, m);
            case PARK -> park(world, x0, y, z0, size, random, m);
        }
    }

    private enum Roof { PLAIN, OFFICE, HELIPAD, WATER_TANK }

    /**
     * A multi-storey building: glass lobby with double doors, then floors of windows between pilasters and
     * floor bands, interior floors with desks and ceiling lights, a parapet roof. Tall ones step back in tiers.
     */
    private static void building(ServerLevelAccessor world, int x0, int y, int z0, int w, int d, int h, Style st, boolean setbacks,
                                 Roof roof, RandomSource random, BlockPos.MutableBlockPos m) {
        int tx = x0, tz = z0, tw = w, td = d, top = h;
        if (setbacks && h >= 20) {
            int t1 = Math.max(8, (h * 3 / 5) / 4 * 4);
            shell(world, x0, y, z0, w, d, 0, t1, st, random, m);
            tx += 2; tz += 2; tw -= 4; td -= 4;
            if (h >= 28 && tw >= 9) {
                int t2 = Math.max(t1 + 4, (h * 17 / 20) / 4 * 4);
                shell(world, tx, y, tz, tw, td, t1, t2, st, random, m);
                tx += 2; tz += 2; tw -= 4; td -= 4;
                shell(world, tx, y, tz, tw, td, t2, h, st, random, m);
            } else {
                shell(world, tx, y, tz, tw, td, t1, h, st, random, m);
            }
        } else {
            shell(world, x0, y, z0, w, d, 0, h, st, random, m);
        }
        rooftop(world, tx, y + top + 1, tz, tw, td, roof, st, random, m);
        // entrance: canopy over double doors on the street (north) side
        int door = x0 + w / 2;
        door(world, door - 1, y + 1, z0, DoorHingeSide.LEFT, m);
        door(world, door, y + 1, z0, DoorHingeSide.RIGHT, m);
        for (int dx = -2; dx <= 1; dx++) {
            set(world, m.set(door + dx, y + 4, z0 - 1), Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        }
        set(world, m.set(door - 2, y + 3, z0 - 1), b(Blocks.SEA_LANTERN));
        set(world, m.set(door + 1, y + 3, z0 - 1), b(Blocks.SEA_LANTERN));
    }

    /** Walls/windows/floors from storey height fromH to toH (relative to y). fromH == 0 adds the lobby and ground floor. */
    private static void shell(ServerLevelAccessor world, int x0, int y, int z0, int w, int d, int fromH, int toH, Style st,
                              RandomSource random, BlockPos.MutableBlockPos m) {
        BlockState floor = b(Blocks.POLISHED_ANDESITE);
        BlockState light = b(Blocks.SEA_LANTERN);
        BlockState desk = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        BlockState roof = b(ModBlocks.ROOF_GRAVEL);
        boolean lobby = fromH == 0;
        boolean core = w >= 11 && d >= 11;
        int cx = x0 + w / 2, cz = z0 + d / 2 + 1;
        long litMask = random.nextLong();
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                boolean edge = dx == 0 || dz == 0 || dx == w - 1 || dz == d - 1;
                boolean corner = (dx == 0 || dx == w - 1) && (dz == 0 || dz == d - 1);
                int along = (dz == 0 || dz == d - 1) ? dx : dz;
                int x = x0 + dx, z = z0 + dz;
                if (lobby) set(world, m.set(x, y, z), edge ? st.trim() : floor);
                for (int hh = fromH + 1; hh <= toH; hh++) {
                    BlockState s;
                    if (edge) {
                        if (lobby && hh <= 4) {
                            s = corner || along % 4 == 0 ? st.column() : hh == 4 ? st.trim() : st.lobby();
                        } else {
                            int f = hh % 4;
                            boolean pilaster = corner || along % st.bay() == 0;
                            if (f == 0) s = st.trim();
                            else if (pilaster) s = st.column();
                            else if (f == 1) s = st.wall();
                            else s = st.window();
                        }
                    } else if (core && Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1 && hh % 4 != 0) {
                        // lift/stair core, with a doorway on each floor
                        boolean doorway = x == cx && z == cz - 1 && hh % 4 != 3;
                        s = doorway || (x == cx && z == cz) ? Blocks.AIR.defaultBlockState() : st.trim();
                    } else if (hh % 4 == 0 && hh < toH) {
                        boolean lit = ((litMask >>> ((hh / 4) % 60)) & 3) != 0;   // most floors have the lights on
                        s = (dx % 4 == 2 && dz % 4 == 2) ? (lit ? light : floor) : floor;
                    } else if (hh % 4 == 1 && hh > 4 && dx % 3 == 1 && dz % 4 == 2 && dx > 1 && dx < w - 2 && dz > 1 && dz < d - 2) {
                        s = desk;   // office desks seen through the windows
                    } else {
                        continue;
                    }
                    set(world, m.set(x, y + hh, z), s);
                }
                // roof slab and a two-high parapet
                set(world, m.set(x, y + toH + 1, z), edge ? st.wall() : roof);
                if (edge) set(world, m.set(x, y + toH + 2, z), st.trim());
            }
        }
        if (lobby) {
            // reception desk and lights in the lobby
            for (int dx = -2; dx <= 2; dx++) set(world, m.set(x0 + w / 2 + dx, y + 1, z0 + 3), b(Blocks.POLISHED_DIORITE));
            for (int dx = 2; dx < w - 2; dx += 4) set(world, m.set(x0 + dx, y + 4, z0 + d / 2), light);
        }
    }

    private static void rooftop(ServerLevelAccessor world, int x0, int ry, int z0, int w, int d, Roof roof, Style st,
                                RandomSource random, BlockPos.MutableBlockPos m) {
        int acs = 1 + random.nextInt(3);
        for (int i = 0; i < acs; i++) {
            set(world, m.set(x0 + 2 + random.nextInt(Math.max(1, w - 4)), ry + 1, z0 + 2 + random.nextInt(Math.max(1, d - 4))),
                    prop(ModBlocks.AC_UNIT, Direction.Plane.HORIZONTAL.getRandomDirection(random)));
        }
        if (w < 7 || d < 7) return;
        // stair bulkhead (not on helipads)
        if (roof != Roof.HELIPAD) for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                for (int h = 1; h <= 3; h++) set(world, m.set(x0 + 1 + dx, ry + h, z0 + d - 4 + dz), h == 3 ? st.trim() : st.wall());
            }
        }
        switch (roof) {
            case HELIPAD -> {
                int cx = x0 + w / 2, cz = z0 + d / 2;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        boolean ring = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                        boolean hBar = (Math.abs(dx) == 1 && Math.abs(dz) <= 2) || (dz == 0 && Math.abs(dx) <= 1);
                        set(world, m.set(cx + dx, ry, cz + dz), ring ? b(Blocks.YELLOW_CONCRETE) : hBar ? b(Blocks.WHITE_CONCRETE) : b(Blocks.GRAY_CONCRETE));
                        set(world, m.set(cx + dx, ry + 1, cz + dz), Blocks.AIR.defaultBlockState());
                    }
                }
                neonCrown(world, x0, ry + 1, z0, Math.min(w, d), random, m);
            }
            case WATER_TANK -> {
                int tx = x0 + w - 5, tz = z0 + 1;
                for (int dx = 0; dx < 3; dx++) {
                    for (int dz = 0; dz < 3; dz++) {
                        boolean cornerPost = (dx != 1) && (dz != 1);
                        if (cornerPost) for (int h = 1; h <= 2; h++) set(world, m.set(tx + dx, ry + h, tz + dz), b(Blocks.IRON_BARS));
                        if (cornerPost) continue;
                        for (int h = 3; h <= 6; h++) set(world, m.set(tx + dx, ry + h, tz + dz), b(Blocks.SPRUCE_PLANKS));
                    }
                }
                for (int dx = 0; dx < 3; dx++) for (int dz = 0; dz < 3; dz++) {
                    if (dx != 1 && dz != 1) for (int h = 3; h <= 6; h++) set(world, m.set(tx + dx, ry + h, tz + dz), b(Blocks.SPRUCE_LOG));
                    set(world, m.set(tx + dx, ry + 7, tz + dz), b(Blocks.SPRUCE_SLAB));
                }
            }
            case OFFICE -> {
                // antenna with a red aviation light
                int ax = x0 + w - 3, az = z0 + 2;
                for (int h = 1; h <= 5; h++) set(world, m.set(ax, ry + h, az), b(Blocks.IRON_BARS));
                set(world, m.set(ax, ry + 6, az), ModBlocks.NEON[0].defaultBlockState());
                set(world, m.set(ax, ry + 7, az), b(Blocks.LIGHTNING_ROD));
            }
            case PLAIN -> { }
        }
    }

    /** Glass balconies on the street side of apartments, one per bay per floor. */
    private static void balconies(ServerLevelAccessor world, int x0, int y, int z0, int w, int h, Style st, RandomSource random,
                                  BlockPos.MutableBlockPos m) {
        BlockState slab = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        for (int hh = 8; hh < h; hh += 4) {
            for (int bay = st.bay(); bay + st.bay() < w; bay += st.bay()) {
                if (random.nextInt(3) == 0) continue;
                for (int dx = 1; dx < st.bay(); dx++) {
                    set(world, m.set(x0 + bay + dx - st.bay() + st.bay(), y + hh, z0 - 1), slab);
                    set(world, m.set(x0 + bay + dx - st.bay() + st.bay(), y + hh + 1, z0 - 1), b(Blocks.WHITE_STAINED_GLASS));
                }
            }
        }
        // shopfront on the ground floor (doors are added by building())
        neonSign(world, x0, y + 5, z0, w, random, m);
    }

    /** Two-storey shop with a glass shopfront, awning and neon sign, shelves and a counter inside, a flat above. */
    private static void shop(ServerLevelAccessor world, int x0, int y, int z0, int size, RandomSource random, BlockPos.MutableBlockPos m) {
        Style st = apartmentStyle(random);
        st = new Style(st.wall(), st.trim(), st.column(), st.window(), b(Blocks.GLASS), 3);
        int bx = x0 + 2, bz = z0 + 3, w = size - 4, d = size - 5;
        building(world, bx, y, bz, w, d, 8, st, false, Roof.PLAIN, random, m);
        for (int dx = 0; dx < w; dx++) set(world, m.set(bx + dx, y + 4, bz - 1), ModBlocks.SHOP_AWNING.defaultBlockState());
        neonSign(world, bx, y + 5, bz, w, random, m);
        // counter with the terminal, shelves along the back wall
        for (int dx = 2; dx < w - 2; dx++) set(world, m.set(bx + dx, y + 1, bz + d - 5), b(Blocks.POLISHED_DIORITE));
        set(world, m.set(bx + w / 2, y + 1, bz + d - 5), ModBlocks.SHOP_TERMINAL.defaultBlockState().setValue(ShopTerminalBlock.FACING, Direction.NORTH));
        for (int dx = 1; dx < w - 1; dx++) {
            for (int h = 1; h <= 2; h++) set(world, m.set(bx + dx, y + h, bz + d - 2), dx % 3 == 0 ? b(Blocks.BARREL) : b(Blocks.BOOKSHELF));
        }
        set(world, m.set(x0 + 3, y + 1, z0 + 1), prop(ModBlocks.VENDING_MACHINE, Direction.NORTH));
        set(world, m.set(x0 + size - 4, y + 1, z0 + 1), prop(ModBlocks.TRASH_CAN, Direction.NORTH));
        set(world, m.set(x0 + size - 2, y + 1, z0 + size - 2), prop(ModBlocks.DUMPSTER, Direction.WEST));
    }

    private static void gasStation(ServerLevelAccessor world, int x0, int y, int z0, int size, RandomSource random, BlockPos.MutableBlockPos m) {
        for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) set(world, m.set(x0 + dx, y, z0 + dz), b(ModBlocks.ASPHALT));
        // canopy on four pillars, lit underneath, red band round the edge
        int cx0 = x0 + 2, cz0 = z0 + 1, cw = size - 4, cd = 9;
        for (int[] p : new int[][]{{cx0 + 1, cz0 + 1}, {cx0 + cw - 2, cz0 + 1}, {cx0 + 1, cz0 + cd - 2}, {cx0 + cw - 2, cz0 + cd - 2}}) {
            for (int h = 1; h <= 4; h++) set(world, m.set(p[0], y + h, p[1]), b(Blocks.QUARTZ_PILLAR));
        }
        for (int dx = 0; dx < cw; dx++) {
            for (int dz = 0; dz < cd; dz++) {
                boolean edge = dx == 0 || dz == 0 || dx == cw - 1 || dz == cd - 1;
                set(world, m.set(cx0 + dx, y + 5, cz0 + dz), edge ? b(Blocks.RED_CONCRETE) : (dx % 3 == 1 && dz % 3 == 1) ? b(Blocks.SEA_LANTERN) : b(Blocks.WHITE_CONCRETE));
            }
        }
        for (int dx = 2; dx < cw - 2; dx++) set(world, m.set(cx0 + dx, y + 6, cz0), ModBlocks.NEON[random.nextInt(ModBlocks.NEON.length)].defaultBlockState());
        // pump islands
        for (int px : new int[]{cx0 + 4, cx0 + cw - 5}) {
            for (int dz = 3; dz <= 5; dz++) set(world, m.set(px, y, cz0 + dz), b(Blocks.SMOOTH_STONE));
            set(world, m.set(px, y + 1, cz0 + 4), b(Blocks.LIGHT_GRAY_CONCRETE));
            set(world, m.set(px, y + 2, cz0 + 4), b(Blocks.RED_CONCRETE));
        }
        // convenience store at the back
        Style st = new Style(b(Blocks.WHITE_CONCRETE), b(Blocks.RED_CONCRETE), b(Blocks.WHITE_CONCRETE), b(Blocks.GLASS), b(Blocks.GLASS), 3);
        int sz = z0 + 11, sw = size - 4;
        building(world, x0 + 2, y, sz, sw, size - 12, 4, st, false, Roof.PLAIN, random, m);
        set(world, m.set(x0 + 2 + sw / 2 + 2, y + 1, sz + 2), ModBlocks.SHOP_TERMINAL.defaultBlockState().setValue(ShopTerminalBlock.FACING, Direction.NORTH));
        set(world, m.set(x0 + 1, y + 1, sz), prop(ModBlocks.VENDING_MACHINE, Direction.NORTH));
    }

    private static void parking(ServerLevelAccessor world, int x0, int y, int z0, int size, RandomSource random, BlockPos.MutableBlockPos m) {
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                boolean stripe = (dz <= 5 || dz >= size - 6) && dx % 3 == 0 && dz != 0 && dz != size - 1;
                set(world, m.set(x0 + dx, y, z0 + dz), stripe ? b(Blocks.WHITE_CONCRETE) : b(ModBlocks.ASPHALT));
            }
        }
        streetLight(world, x0 + size / 2, y, z0 + size / 2, m);
        int cars = 2 + random.nextInt(3);
        for (int c = 0; c < cars; c++) {
            boolean north = random.nextBoolean();
            int slot = random.nextInt((size - 1) / 3);
            double x = x0 + slot * 3 + 2.0, z = north ? z0 + 3.0 : z0 + size - 3.0;
            float yaw = north ? 180.0f : 0.0f;
            CarEntity car = (random.nextInt(4) == 0 ? ModEntities.SPORTS_CAR : ModEntities.SEDAN).create(world.getLevel(),
                    net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
            if (car == null) continue;
            car.snapTo(x, y + 1, z, yaw, 0.0f);
            car.setYHeadRot(yaw);
            car.setYBodyRot(yaw);
            car.initFresh(random.nextInt(CarEntity.COLORS.length));
            car.setCivilian(true);
            world.addFreshEntityWithPassengers(car);
        }
    }

    private static void park(ServerLevelAccessor world, int x0, int y, int z0, int size, RandomSource random, BlockPos.MutableBlockPos m) {
        int c = size / 2;
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                boolean path = Math.abs(dx - c) <= 1 || Math.abs(dz - c) <= 1;
                if (path) set(world, m.set(x0 + dx, y, z0 + dz), b(Blocks.GRAVEL));
                else if (random.nextInt(7) == 0) set(world, m.set(x0 + dx, y + 1, z0 + dz), b(Blocks.SHORT_GRASS));
                else if (random.nextInt(25) == 0) set(world, m.set(x0 + dx, y + 1, z0 + dz), b(Blocks.POPPY));
            }
        }
        // fountain
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                set(world, m.set(x0 + c + dx, y, z0 + c + dz), b(Blocks.STONE_BRICKS));
                set(world, m.set(x0 + c + dx, y + 1, z0 + c + dz), rim ? b(Blocks.STONE_BRICK_SLAB) : b(Blocks.WATER));
            }
        }
        set(world, m.set(x0 + c, y + 1, z0 + c), b(Blocks.CHISELED_STONE_BRICKS));
        set(world, m.set(x0 + c, y + 2, z0 + c), b(Blocks.WATER));
        for (int[] t : new int[][]{{3, 3}, {size - 4, 3}, {3, size - 4}, {size - 4, size - 4}}) tree(world, x0 + t[0], y, z0 + t[1], m);
        set(world, m.set(x0 + c - 4, y + 1, z0 + c + 2), prop(ModBlocks.BENCH, Direction.EAST));
        set(world, m.set(x0 + c + 4, y + 1, z0 + c - 2), prop(ModBlocks.BENCH, Direction.WEST));
        set(world, m.set(x0 + c + 2, y + 1, z0 + c + 4), prop(ModBlocks.TRASH_CAN, Direction.NORTH));
        streetLight(world, x0 + c - 2, y, z0 + c - 4, m);
        streetLight(world, x0 + c + 2, y, z0 + c + 4, m);
    }

    private static void flagpole(ServerLevelAccessor world, int x, int y, int z, BlockPos.MutableBlockPos m) {
        for (int h = 1; h <= 7; h++) set(world, m.set(x, y + h, z), b(Blocks.IRON_BARS));
        set(world, m.set(x + 1, y + 7, z), b(Blocks.BLUE_WOOL));
        set(world, m.set(x + 2, y + 7, z), b(Blocks.WHITE_WOOL));
        set(world, m.set(x + 1, y + 6, z), b(Blocks.WHITE_WOOL));
        set(world, m.set(x + 2, y + 6, z), b(Blocks.BLUE_WOOL));
    }

    private static void door(ServerLevelAccessor world, int x, int y, int z, DoorHingeSide hinge, BlockPos.MutableBlockPos m) {
        BlockState lower = Blocks.WAXED_COPPER_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HINGE, hinge).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        set(world, m.set(x, y, z), lower);
        set(world, m.set(x, y + 1, z), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void neonCrown(ServerLevelAccessor world, int x0, int y, int z0, int w, RandomSource random, BlockPos.MutableBlockPos m) {
        BlockState neon = ModBlocks.NEON[random.nextInt(ModBlocks.NEON.length)].defaultBlockState();
        for (int i = 0; i < w; i++) {
            set(world, m.set(x0 + i, y, z0), neon);
            set(world, m.set(x0 + i, y, z0 + w - 1), neon);
            set(world, m.set(x0, y, z0 + i), neon);
            set(world, m.set(x0 + w - 1, y, z0 + i), neon);
        }
    }

    private static void neonSign(ServerLevelAccessor world, int x0, int y, int z0, int w, RandomSource random, BlockPos.MutableBlockPos m) {
        BlockState neon = ModBlocks.NEON[random.nextInt(ModBlocks.NEON.length)].defaultBlockState();
        for (int dx = 3; dx < w - 3; dx++) set(world, m.set(x0 + dx, y, z0 - 1), neon);
    }

    private static void spawnTraffic(ServerLevelAccessor world, int x0, int y, int z0, RandomSource random) {
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
            CarEntity car = type.create(world.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
            if (car == null) continue;
            car.snapTo(x, y + 1, z, yaw, 0.0f);
            car.setYHeadRot(yaw);
            car.setYBodyRot(yaw);
            car.initFresh(random.nextInt(CarEntity.COLORS.length));
            car.setCivilian(true);
            world.addFreshEntityWithPassengers(car);
        }
        int people = 2 + random.nextInt(4);
        int blockX = Math.floorDiv(x0 + OFF, PITCH) * PITCH - OFF;
        int blockZ = Math.floorDiv(z0 + OFF, PITCH) * PITCH - OFF;
        for (int p = 0; p < people; p++) {
            CitizenEntity citizen = ModEntities.CITIZEN.create(world.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
            if (citizen == null) continue;
            double px = blockX + PITCH * random.nextInt(2) + ROAD + 1 + random.nextInt(PITCH - ROAD - 2) + 0.5;
            double pz = blockZ + PITCH * random.nextInt(2) + ROAD + 0.5;
            if (px < x0 + 1 || px > x0 + SIZE - 1 || pz < z0 + 1 || pz > z0 + SIZE - 1) continue;
            citizen.snapTo(px, y + 1, pz, random.nextFloat() * 360.0f, 0.0f);
            citizen.randomize();
            citizen.setPersistenceRequired();
            world.addFreshEntityWithPassengers(citizen);
        }
    }

    private static BlockState prop(Block block, Direction facing) {
        return block.defaultBlockState().setValue(PropBlock.FACING, facing);
    }

    private static void trafficLight(ServerLevelAccessor world, int x, int y, int z, Direction facing, BlockPos.MutableBlockPos m) {
        for (int h = 1; h <= 3; h++) set(world, m.set(x, y + h, z), Blocks.IRON_BARS.defaultBlockState());
        set(world, m.set(x, y + 4, z), prop(ModBlocks.TRAFFIC_LIGHT, facing));
    }

    private static void streetLight(ServerLevelAccessor world, int x, int y, int z, BlockPos.MutableBlockPos m) {
        for (int h = 1; h <= 4; h++) set(world, m.set(x, y + h, z), Blocks.IRON_BARS.defaultBlockState());
        set(world, m.set(x, y + 5, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
    }

    private static void tree(ServerLevelAccessor world, int x, int y, int z, BlockPos.MutableBlockPos m) {
        for (int h = 1; h <= 4; h++) set(world, m.set(x, y + h, z), Blocks.OAK_LOG.defaultBlockState());
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
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

    private static void set(ServerLevelAccessor world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
