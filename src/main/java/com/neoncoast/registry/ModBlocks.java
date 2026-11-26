package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import com.neoncoast.block.ChopShopBlock;
import com.neoncoast.block.JobBoardBlock;
import com.neoncoast.block.PropBlock;
import com.neoncoast.block.ShopTerminalBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Block ASPHALT = register("asphalt", new Block(stone(MapColor.COLOR_BLACK)));
    public static final Block ROAD_LINE = register("road_line", new Block(stone(MapColor.COLOR_BLACK)));
    public static final Block SIDEWALK = register("sidewalk", new Block(stone(MapColor.COLOR_LIGHT_GRAY)));
    public static final Block SHOP_TERMINAL = register("shop_terminal", new ShopTerminalBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN).strength(3.0f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 8)));
    public static final Block JOB_BOARD = register("job_board", new JobBoardBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD)));
    public static final Block CHOP_SHOP = register("chop_shop", new ChopShopBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).strength(3.0f).sound(SoundType.METAL)));

    // ---- building blocks
    public static final Block CONCRETE_PANEL = register("concrete_panel", new Block(stone(MapColor.COLOR_LIGHT_GRAY)));
    public static final Block DARK_PANEL = register("dark_panel", new Block(stone(MapColor.COLOR_GRAY)));
    public static final Block MARBLE = register("marble", new Block(stone(MapColor.SNOW)));
    public static final Block BRICK_FACADE = register("brick_facade", new Block(stone(MapColor.COLOR_RED)));
    public static final Block METAL_CLADDING = register("metal_cladding", new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL)));
    public static final Block ROOF_GRAVEL = register("roof_gravel", new Block(stone(MapColor.STONE)));
    public static final Block GARAGE_DOOR = register("garage_door", new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));
    public static final Block SHOP_AWNING = register("shop_awning", new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).strength(0.8f).sound(SoundType.WOOL)));
    public static final Block OFFICE_WINDOW = register("office_window", glass());
    public static final Block GLASS_CURTAIN_WALL = register("glass_curtain_wall", glass());
    public static final Block CROSSWALK = register("crosswalk", new Block(stone(MapColor.COLOR_BLACK)));
    public static final Block MANHOLE = register("manhole", new Block(stone(MapColor.COLOR_BLACK)));
    public static final String[] NEON_COLORS = {"red", "blue", "pink", "green", "yellow"};
    public static final Block[] NEON = new Block[NEON_COLORS.length];

    static {
        for (int i = 0; i < NEON_COLORS.length; i++) {
            NEON[i] = register("neon_" + NEON_COLORS[i], new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f).sound(SoundType.GLASS).lightLevel(st -> 15).instrument(NoteBlockInstrument.HAT)));
        }
    }

    // ---- street props
    public static final Block VENDING_MACHINE = register("vending_machine", new PropBlock(prop(MapColor.COLOR_RED).lightLevel(st -> 6), 1, 0, 2, 15, 16, 14));
    public static final Block DUMPSTER = register("dumpster", new PropBlock(prop(MapColor.COLOR_GREEN), 0, 0, 2, 16, 12, 14));
    public static final Block AC_UNIT = register("ac_unit", new PropBlock(prop(MapColor.METAL), 1, 0, 1, 15, 10, 15));
    public static final Block FIRE_HYDRANT = register("fire_hydrant", new PropBlock(prop(MapColor.COLOR_RED), 5, 0, 5, 11, 11, 11));
    public static final Block TRAFFIC_LIGHT = register("traffic_light", new PropBlock(prop(MapColor.COLOR_BLACK).lightLevel(st -> 5), 5, 0, 5, 11, 16, 11));
    public static final Block BENCH = register("bench", new PropBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(1.5f).sound(SoundType.WOOD).noOcclusion(), 0, 0, 4, 16, 8, 12));
    public static final Block TRASH_CAN = register("trash_can", new PropBlock(prop(MapColor.COLOR_GRAY), 4, 0, 4, 12, 12, 12));

    private static BlockBehaviour.Properties prop(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(2.0f).sound(SoundType.METAL).noOcclusion();
    }

    private static Block glass() {
        return new TransparentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6f)
                .sound(SoundType.GLASS).noOcclusion().isValidSpawn((st, w, p, t) -> false)
                .isRedstoneConductor((st, w, p) -> false).isSuffocating((st, w, p) -> false).isViewBlocking((st, w, p) -> false));
    }

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties stone(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(1.5f, 6.0f).sound(SoundType.STONE);
    }

    private static Block register(String name, Block block) {
        Block registered = Registry.register(BuiltInRegistries.BLOCK, NeonCoast.id(name), block);
        BlockItem item = new BlockItem(registered, new Item.Properties());
        item.registerBlocks(Item.BY_BLOCK, item);
        Registry.register(BuiltInRegistries.ITEM, NeonCoast.id(name), item);
        return registered;
    }

    public static void init() {
    }
}
