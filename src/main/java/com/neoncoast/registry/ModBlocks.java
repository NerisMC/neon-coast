package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import com.neoncoast.block.ChopShopBlock;
import com.neoncoast.block.JobBoardBlock;
import com.neoncoast.block.PropBlock;
import com.neoncoast.block.ShopTerminalBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    public static final Block ASPHALT = register("asphalt", new Block(stone(MapColor.BLACK)));
    public static final Block ROAD_LINE = register("road_line", new Block(stone(MapColor.BLACK)));
    public static final Block SIDEWALK = register("sidewalk", new Block(stone(MapColor.LIGHT_GRAY)));
    public static final Block SHOP_TERMINAL = register("shop_terminal", new ShopTerminalBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.GREEN).strength(3.0f).sounds(BlockSoundGroup.METAL).nonOpaque().luminance(s -> 8)));
    public static final Block JOB_BOARD = register("job_board", new JobBoardBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.OAK_TAN).strength(2.0f).sounds(BlockSoundGroup.WOOD)));
    public static final Block CHOP_SHOP = register("chop_shop", new ChopShopBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.GRAY).strength(3.0f).sounds(BlockSoundGroup.METAL)));

    // ---- building blocks
    public static final Block CONCRETE_PANEL = register("concrete_panel", new Block(stone(MapColor.LIGHT_GRAY)));
    public static final Block DARK_PANEL = register("dark_panel", new Block(stone(MapColor.GRAY)));
    public static final Block MARBLE = register("marble", new Block(stone(MapColor.WHITE)));
    public static final Block BRICK_FACADE = register("brick_facade", new Block(stone(MapColor.RED)));
    public static final Block METAL_CLADDING = register("metal_cladding", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.IRON_GRAY).strength(3.0f, 6.0f).sounds(BlockSoundGroup.METAL)));
    public static final Block ROOF_GRAVEL = register("roof_gravel", new Block(stone(MapColor.STONE_GRAY)));
    public static final Block GARAGE_DOOR = register("garage_door", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.IRON_GRAY).strength(3.0f).sounds(BlockSoundGroup.METAL)));
    public static final Block SHOP_AWNING = register("shop_awning", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.RED).strength(0.8f).sounds(BlockSoundGroup.WOOL)));
    public static final Block OFFICE_WINDOW = register("office_window", glass());
    public static final Block GLASS_CURTAIN_WALL = register("glass_curtain_wall", glass());
    public static final Block CROSSWALK = register("crosswalk", new Block(stone(MapColor.BLACK)));
    public static final Block MANHOLE = register("manhole", new Block(stone(MapColor.BLACK)));
    public static final String[] NEON_COLORS = {"red", "blue", "pink", "green", "yellow"};
    public static final Block[] NEON = new Block[NEON_COLORS.length];

    static {
        for (int i = 0; i < NEON_COLORS.length; i++) {
            NEON[i] = register("neon_" + NEON_COLORS[i], new Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE)
                    .strength(0.5f).sounds(BlockSoundGroup.GLASS).luminance(st -> 15).instrument(NoteBlockInstrument.HAT)));
        }
    }

    // ---- street props
    public static final Block VENDING_MACHINE = register("vending_machine", new PropBlock(prop(MapColor.RED).luminance(st -> 6), 1, 0, 2, 15, 16, 14));
    public static final Block DUMPSTER = register("dumpster", new PropBlock(prop(MapColor.GREEN), 0, 0, 2, 16, 12, 14));
    public static final Block AC_UNIT = register("ac_unit", new PropBlock(prop(MapColor.IRON_GRAY), 1, 0, 1, 15, 10, 15));
    public static final Block FIRE_HYDRANT = register("fire_hydrant", new PropBlock(prop(MapColor.RED), 5, 0, 5, 11, 11, 11));
    public static final Block TRAFFIC_LIGHT = register("traffic_light", new PropBlock(prop(MapColor.BLACK).luminance(st -> 5), 5, 0, 5, 11, 16, 11));
    public static final Block BENCH = register("bench", new PropBlock(AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN)
            .strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque(), 0, 0, 4, 16, 8, 12));
    public static final Block TRASH_CAN = register("trash_can", new PropBlock(prop(MapColor.GRAY), 4, 0, 4, 12, 12, 12));

    private static AbstractBlock.Settings prop(MapColor color) {
        return AbstractBlock.Settings.create().mapColor(color).strength(2.0f).sounds(BlockSoundGroup.METAL).nonOpaque();
    }

    private static Block glass() {
        return new TransparentBlock(AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_BLUE).strength(0.6f)
                .sounds(BlockSoundGroup.GLASS).nonOpaque().allowsSpawning((st, w, p, t) -> false)
                .solidBlock((st, w, p) -> false).suffocates((st, w, p) -> false).blockVision((st, w, p) -> false));
    }

    private ModBlocks() {
    }

    private static AbstractBlock.Settings stone(MapColor color) {
        return AbstractBlock.Settings.create().mapColor(color).strength(1.5f, 6.0f).sounds(BlockSoundGroup.STONE);
    }

    private static Block register(String name, Block block) {
        Block registered = Registry.register(Registries.BLOCK, NeonCoast.id(name), block);
        BlockItem item = new BlockItem(registered, new Item.Settings());
        item.appendBlocks(Item.BLOCK_ITEMS, item);
        Registry.register(Registries.ITEM, NeonCoast.id(name), item);
        return registered;
    }

    public static void init() {
    }
}
