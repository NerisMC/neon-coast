package com.pipsparty.world;

import com.pipsparty.block.HallLightBlock;
import com.pipsparty.block.PropBlock;
import com.pipsparty.block.SecurityDoorBlock;
import com.pipsparty.block.WallPropBlock;
import com.pipsparty.entity.AnimatronicEntity;
import com.pipsparty.entity.Character;
import com.pipsparty.entity.SecurityCameraEntity;
import com.pipsparty.registry.ModBlocks;
import com.pipsparty.registry.ModEntities;
import com.pipsparty.registry.ModItems;
import com.pipsparty.registry.Reg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a Pip's Party Palace, 40 x 34 blocks, from its north-west corner. Local coordinates: lx grows east,
 * lz grows south; the front door is in the middle of the south wall.
 *
 * <pre>
 *  lz 0  +----------------------------------------+
 *        | (closet) STAGE (lx 6-20)  |BACKSTAGE|WEST HALL ->| OFFICE  |
 *        |                           |  23-26  |  28-32     | 34-38   |
 *  lz 8  |                           |---------+------------+--door---|
 *        |GATOR  |                   PARTY ROOM (28-33)   |EAST HALL|
 *        |GROTTO |  DINING HALL                           | 35-37   |
 *  lz 19 |-------|  party tables                 ---------+-opening-|
 *        |                                       | KITCHEN (28-38)   |
 *        |ARCADE + ball pit                      |                   |
 *  lz 33 +--------------- front door (lx 18-20) -+-------------------+
 * </pre>
 */
public final class PizzeriaGenerator {
    public static final int WIDTH = 40;
    public static final int DEPTH = 34;
    private static final int HEIGHT = 6;   // ceiling at y + 6

    private PizzeriaGenerator() {
    }

    private record Ctx(ServerLevelAccessor world, int x0, int y, int z0, RandomSource random, BlockPos.MutableBlockPos m) {
        BlockPos at(int lx, int dy, int lz) {
            return m.set(x0 + lx, y + dy, z0 + lz);
        }

        void set(int lx, int dy, int lz, BlockState s) {
            world.setBlock(at(lx, dy, lz), s, Block.UPDATE_CLIENTS);
        }

        void fill(int lx1, int lz1, int lx2, int lz2, int dy1, int dy2, BlockState s) {
            for (int lx = Math.min(lx1, lx2); lx <= Math.max(lx1, lx2); lx++) {
                for (int lz = Math.min(lz1, lz2); lz <= Math.max(lz1, lz2); lz++) {
                    for (int dy = dy1; dy <= dy2; dy++) set(lx, dy, lz, s);
                }
            }
        }
    }

    private static BlockState b(Reg<Block> r) {
        return r.get().defaultBlockState();
    }

    private static BlockState facing(Reg<Block> r, Direction d) {
        BlockState s = r.get().defaultBlockState();
        if (s.getBlock() instanceof PropBlock) return s.setValue(PropBlock.FACING, d);
        if (s.getBlock() instanceof WallPropBlock) return s.setValue(WallPropBlock.FACING, d);
        return s;
    }

    /** @param nw north-west corner at ground level (the floor goes here) */
    public static void generate(ServerLevelAccessor world, BlockPos nw, RandomSource random) {
        Ctx c = new Ctx(world, nw.getX(), nw.getY(), nw.getZ(), random, new BlockPos.MutableBlockPos());
        BlockState air = Blocks.AIR.defaultBlockState();

        // ---- site: foundation, clear the air, floor, ceiling
        for (int lx = -1; lx <= WIDTH; lx++) {
            for (int lz = -1; lz <= DEPTH; lz++) {
                for (int dy = 1; dy <= HEIGHT + 4; dy++) c.set(lx, dy, lz, air);
                for (int dy = -1; dy >= -6; dy--) {
                    BlockState below = world.getBlockState(c.at(lx, dy, lz));
                    if (below.canBeReplaced() || !below.getFluidState().isEmpty()) c.set(lx, dy, lz, Blocks.STONE.defaultBlockState());
                    else break;
                }
                boolean inside = lx >= 0 && lx < WIDTH && lz >= 0 && lz < DEPTH;
                c.set(lx, 0, lz, inside ? b(ModBlocks.CHECKER_FLOOR) : Blocks.GRASS_BLOCK.defaultBlockState());
                if (inside) c.set(lx, HEIGHT, lz, b(ModBlocks.CEILING_TILE));
            }
        }
        // ---- outer walls: red below, a checkered trim band, purple above
        for (int lx = 0; lx < WIDTH; lx++) {
            for (int lz = 0; lz < DEPTH; lz++) {
                if (lx != 0 && lz != 0 && lx != WIDTH - 1 && lz != DEPTH - 1) continue;
                for (int dy = 1; dy < HEIGHT; dy++) {
                    c.set(lx, dy, lz, dy <= 2 ? b(ModBlocks.PARTY_WALL_RED) : dy == 3 ? b(ModBlocks.PARTY_WALL_TRIM) : b(ModBlocks.PARTY_WALL_PURPLE));
                }
                c.set(lx, HEIGHT, lz, b(ModBlocks.PARTY_WALL_PURPLE));
            }
        }
        // front door
        c.fill(18, DEPTH - 1, 20, DEPTH - 1, 1, 3, air);
        c.set(19, 4, DEPTH - 2, facing(ModBlocks.EXIT_SIGN, Direction.NORTH));

        BlockState wall = b(ModBlocks.PARTY_WALL_BLUE);
        BlockState office = b(ModBlocks.OFFICE_WALL);

        // ---- office (north-east corner) with two security doors
        c.fill(33, 1, 33, 8, 1, HEIGHT - 1, office);
        c.fill(33, 8, 38, 8, 1, HEIGHT - 1, office);
        c.fill(34, 1, 38, 7, 0, 0, Blocks.POLISHED_ANDESITE.defaultBlockState());
        c.fill(33, 5, 33, 5, 1, 2, b(ModBlocks.SECURITY_DOOR));                       // west (left) door
        c.fill(36, 8, 36, 8, 1, 2, b(ModBlocks.SECURITY_DOOR));                       // south (right) door
        c.set(33, 2, 4, b(ModBlocks.DOOR_BUTTON));
        c.set(33, 2, 6, b(ModBlocks.LIGHT_BUTTON));
        c.set(37, 2, 8, b(ModBlocks.DOOR_BUTTON));
        c.set(38, 2, 8, b(ModBlocks.LIGHT_BUTTON));
        c.set(35, 1, 2, facing(ModBlocks.SECURITY_DESK, Direction.SOUTH));
        c.set(36, 1, 2, facing(ModBlocks.SECURITY_DESK, Direction.SOUTH));
        c.set(37, 1, 2, facing(ModBlocks.SECURITY_DESK, Direction.SOUTH));
        c.set(35, 2, 2, facing(ModBlocks.OFFICE_MONITOR, Direction.SOUTH));
        c.set(36, 2, 2, facing(ModBlocks.OFFICE_MONITOR, Direction.SOUTH));
        c.set(37, 2, 2, facing(ModBlocks.DESK_FAN, Direction.SOUTH));
        c.set(38, 1, 6, facing(ModBlocks.TIME_CLOCK, Direction.WEST));
        c.set(38, 3, 3, facing(ModBlocks.POSTER_RULES, Direction.WEST));
        c.set(34, 3, 1, facing(ModBlocks.POSTER_PIP, Direction.SOUTH));
        c.set(36, HEIGHT, 5, Blocks.SEA_LANTERN.defaultBlockState());
        BlockPos chestPos = c.at(34, 1, 2).immutable();
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.CAMERA_TABLET.get()));
            chest.setItem(1, new ItemStack(ModItems.OVERRIDE_CHIP.get()));
            chest.setItem(2, new ItemStack(ModItems.PIZZA_SLICE.get(), 4));
            chest.setItem(3, new ItemStack(ModItems.PARTY_SODA.get(), 2));
        }

        // ---- west hall (to the left door) and the closet above it
        c.fill(28, 1, 32, 2, 1, HEIGHT - 1, wall);
        c.fill(28, 3, 32, 3, 1, HEIGHT - 1, wall);
        c.fill(28, 7, 32, 7, 1, HEIGHT - 1, wall);
        c.set(31, HEIGHT, 5, b(ModBlocks.HALL_LIGHT));
        c.set(29, 3, 4, facing(ModBlocks.POSTER_GNASH, Direction.SOUTH));

        // ---- backstage
        c.fill(22, 1, 22, 8, 1, HEIGHT - 1, wall);
        c.fill(27, 1, 27, 8, 1, HEIGHT - 1, wall);
        c.fill(22, 9, 27, 9, 1, HEIGHT - 1, wall);
        c.fill(22, 4, 22, 4, 1, 2, air);                       // door to the stage
        c.fill(27, 4, 27, 6, 1, 3, air);                       // into the west hall
        for (int lz = 1; lz <= 8; lz += 2) c.set(26, 1, lz, Blocks.BARREL.defaultBlockState());
        c.set(23, 1, 1, Blocks.BARREL.defaultBlockState());
        c.set(23, 2, 1, Blocks.BARREL.defaultBlockState());
        c.set(24, 3, 1, facing(ModBlocks.EMPLOYEES_ONLY_SIGN, Direction.SOUTH));

        // ---- east hall (to the right door)
        c.fill(34, 9, 34, 19, 1, HEIGHT - 1, wall);
        c.fill(38, 9, 38, 19, 1, HEIGHT - 1, wall);
        c.set(36, HEIGHT, 12, b(ModBlocks.HALL_LIGHT));

        // ---- kitchen
        c.fill(28, 19, 38, 19, 1, HEIGHT - 1, wall);
        c.fill(27, 19, 27, DEPTH - 1, 1, HEIGHT - 1, wall);
        c.fill(35, 19, 37, 19, 1, 3, air);                     // from the east hall
        c.fill(27, 26, 27, 27, 1, 2, air);                     // to the dining hall
        c.fill(28, 20, 38, 32, 0, 0, Blocks.SMOOTH_STONE.defaultBlockState());
        for (int lz = 21; lz <= 31; lz++) c.set(38, 1, lz, facing(ModBlocks.KITCHEN_COUNTER, Direction.WEST));
        for (int lx = 30; lx <= 33; lx++) c.set(lx, 1, 32, facing(ModBlocks.PIZZA_OVEN, Direction.NORTH));
        for (int lx = 31; lx <= 34; lx++) c.set(lx, 1, 26, facing(ModBlocks.KITCHEN_COUNTER, Direction.SOUTH));

        // ---- party room (opens onto the dining hall)
        for (int lx = 29; lx <= 32; lx += 3) {
            c.set(lx, 1, 12, facing(ModBlocks.PARTY_TABLE, Direction.SOUTH));
            c.set(lx, 1, 15, facing(ModBlocks.PARTY_TABLE, Direction.SOUTH));
            c.set(lx, 1, 13, facing(ModBlocks.PARTY_CHAIR, Direction.NORTH));
            c.set(lx, 1, 11, facing(ModBlocks.PARTY_CHAIR, Direction.SOUTH));
        }
        c.set(33, 1, 10, b(ModBlocks.GIFT_BOX));
        c.set(33, 1, 17, b(ModBlocks.PARTY_BALLOONS));
        c.set(33, 2, 17, b(ModBlocks.PARTY_BALLOONS));
        c.fill(28, 8, 33, 8, 4, 5, b(ModBlocks.CONFETTI_WALL));

        // ---- show stage
        c.fill(6, 1, 20, 4, 1, 1, b(ModBlocks.STAGE_FLOOR));
        c.fill(6, 1, 20, 1, 2, HEIGHT - 1, b(ModBlocks.STAGE_CURTAIN));
        c.fill(6, 1, 6, 4, 2, HEIGHT - 1, b(ModBlocks.STAGE_CURTAIN));
        c.fill(20, 1, 20, 4, 2, HEIGHT - 1, b(ModBlocks.STAGE_CURTAIN));
        c.set(8, HEIGHT, 5, Blocks.SEA_LANTERN.defaultBlockState());
        c.set(13, HEIGHT, 5, Blocks.SEA_LANTERN.defaultBlockState());
        c.set(18, HEIGHT, 5, Blocks.SEA_LANTERN.defaultBlockState());
        c.fill(1, 1, 5, 10, 1, HEIGHT - 1, wall);              // closet west of the stage

        // ---- Gator Grotto
        c.fill(1, 11, 5, 11, 1, HEIGHT - 1, wall);
        c.fill(1, 19, 5, 19, 1, HEIGHT - 1, wall);
        c.fill(5, 12, 5, 18, 1, HEIGHT - 2, b(ModBlocks.STAR_CURTAIN));
        c.fill(5, 12, 5, 18, HEIGHT - 1, HEIGHT - 1, wall);
        c.fill(1, 12, 4, 18, 0, 0, b(ModBlocks.STAGE_FLOOR));
        c.set(6, 4, 15, facing(ModBlocks.POSTER_GNASH, Direction.EAST));

        // ---- arcade corner
        for (int lz = 25; lz <= 31; lz += 2) c.set(1, 1, lz, facing(ModBlocks.ARCADE_CABINET, Direction.EAST));
        c.fill(3, 28, 6, 31, 1, 1, b(ModBlocks.BALL_PIT));
        c.fill(8, 32, 10, 32, 1, 1, facing(ModBlocks.PRIZE_COUNTER, Direction.NORTH));
        c.set(11, 1, 32, facing(ModBlocks.TICKET_MACHINE, Direction.NORTH));
        c.set(1, 3, 22, facing(ModBlocks.POSTER_RUSTY, Direction.EAST));
        c.set(1, 3, 24, facing(ModBlocks.POSTER_MARLA, Direction.EAST));

        // ---- dining hall: rows of party tables with chairs and balloons
        for (int lx = 11; lx <= 23; lx += 4) {
            for (int lz = 12; lz <= 28; lz += 5) {
                if (lx <= 11 && lz >= 24) continue;
                c.set(lx, 1, lz, facing(ModBlocks.PARTY_TABLE, Direction.SOUTH));
                c.set(lx + 1, 1, lz, facing(ModBlocks.PARTY_TABLE, Direction.SOUTH));
                c.set(lx, 1, lz - 1, facing(ModBlocks.PARTY_CHAIR, Direction.SOUTH));
                c.set(lx + 1, 1, lz - 1, facing(ModBlocks.PARTY_CHAIR, Direction.SOUTH));
                c.set(lx, 1, lz + 1, facing(ModBlocks.PARTY_CHAIR, Direction.NORTH));
                c.set(lx + 1, 1, lz + 1, facing(ModBlocks.PARTY_CHAIR, Direction.NORTH));
                if (random.nextInt(3) == 0) c.set(lx, 2, lz, b(ModBlocks.GIFT_BOX));
            }
        }
        for (int lx = 4; lx <= 24; lx += 6) {
            for (int lz = 12; lz <= 30; lz += 6) c.set(lx, HEIGHT, lz, Blocks.SEA_LANTERN.defaultBlockState());
        }
        c.set(25, 1, 10, b(ModBlocks.PARTY_BALLOONS));
        c.set(25, 2, 10, b(ModBlocks.PARTY_BALLOONS));
        c.set(22, 1, 31, facing(ModBlocks.PIZZERIA_REGISTER, Direction.SOUTH));
        c.set(24, 1, 31, facing(ModBlocks.FRANCHISE_CATALOG, Direction.SOUTH));
        c.set(26, 3, 30, facing(ModBlocks.POSTER_PIP, Direction.WEST));

        // ---- a few years of neglect
        for (int i = 0; i < 10; i++) {
            int lx = 1 + random.nextInt(WIDTH - 2), lz = 1 + random.nextInt(DEPTH - 2);
            if (world.getBlockState(c.at(lx, HEIGHT - 1, lz)).isAir()) c.set(lx, HEIGHT - 1, lz, Blocks.COBWEB.defaultBlockState());
        }

        // ---- the band
        band(c, Character.PIP, 13, 2, 3, 0.0f);
        band(c, Character.RUSTY, 9, 2, 3, 0.0f);
        band(c, Character.MARLA, 17, 2, 3, 0.0f);
        band(c, Character.GNASH, 2, 1, 15, -90.0f);

        // ---- security cameras (yaw: 0 south, 90 west, 180 north, -90 east)
        camera(c, "CAM 1A Show Stage", 13, 14, 180.0f);
        camera(c, "CAM 1B Dining Area", 25, 31, 135.0f);
        camera(c, "CAM 1C Gator Grotto", 7, 15, 90.0f);
        camera(c, "CAM 2A West Hall", 28, 5, -90.0f);
        camera(c, "CAM 2B East Hall", 36, 18, 180.0f);
        camera(c, "CAM 3 Backstage", 25, 7, 135.0f);
        camera(c, "CAM 4 Kitchen", 37, 31, 135.0f);
        camera(c, "CAM 5 Arcade", 9, 31, 135.0f);
        camera(c, "CAM 6 Party Room", 32, 17, 135.0f);
    }

    private static void band(Ctx c, Character ch, int lx, int dy, int lz, float yaw) {
        AnimatronicEntity a = ModEntities.of(ch).get().create(c.world().getLevel(), EntitySpawnReason.STRUCTURE);
        if (a == null) return;
        BlockPos home = new BlockPos(c.x0() + lx, c.y() + dy, c.z0() + lz);
        a.snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, yaw, 0.0f);
        a.setYHeadRot(yaw);
        a.setYBodyRot(yaw);
        a.setHome(home, yaw);
        c.world().addFreshEntity(a);
    }

    private static void camera(Ctx c, String name, int lx, int lz, float yaw) {
        SecurityCameraEntity cam = ModEntities.SECURITY_CAMERA.get().create(c.world().getLevel(), EntitySpawnReason.STRUCTURE);
        if (cam == null) return;
        cam.snapTo(c.x0() + lx + 0.5, c.y() + HEIGHT - 0.75, c.z0() + lz + 0.5, yaw, 25.0f);
        cam.setCustomName(Component.literal(name));
        c.world().addFreshEntity(cam);
    }

    /** True if every corner of the footprint is within 3 blocks of the given ground height and dry. */
    public static boolean siteOk(ServerLevelAccessor world, BlockPos nw, java.util.function.ToIntBiFunction<Integer, Integer> groundAt) {
        int[][] corners = {{0, 0}, {WIDTH - 1, 0}, {0, DEPTH - 1}, {WIDTH - 1, DEPTH - 1}, {WIDTH / 2, DEPTH / 2}};
        for (int[] p : corners) {
            int g = groundAt.applyAsInt(nw.getX() + p[0], nw.getZ() + p[1]);
            if (Math.abs(g - nw.getY()) > 3) return false;
            if (!world.getFluidState(new BlockPos(nw.getX() + p[0], g, nw.getZ() + p[1])).isEmpty()) return false;
        }
        return true;
    }

    @SuppressWarnings("unused")
    private static BlockState lit(Reg<Block> r) {
        return r.get().defaultBlockState().setValue(HallLightBlock.LIT, true);
    }

    @SuppressWarnings("unused")
    private static BlockState closed(Reg<Block> r) {
        return r.get().defaultBlockState().setValue(SecurityDoorBlock.CLOSED, true);
    }
}
