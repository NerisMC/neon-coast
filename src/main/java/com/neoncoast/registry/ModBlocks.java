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
    public static final Block ASPHALT = register("asphalt", Block::new, stone(MapColor.COLOR_BLACK));
    public static final Block ROAD_LINE = register("road_line", Block::new, stone(MapColor.COLOR_BLACK));
    public static final Block SIDEWALK = register("sidewalk", Block::new, stone(MapColor.COLOR_LIGHT_GRAY));
    public static final Block SHOP_TERMINAL = register("shop_terminal", ShopTerminalBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN).strength(3.0f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 8));
    public static final Block JOB_BOARD = register("job_board", JobBoardBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD));
    public static final Block CHOP_SHOP = register("chop_shop", ChopShopBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).strength(3.0f).sound(SoundType.METAL));

    // ---- building blocks
    public static final Block CONCRETE_PANEL = register("concrete_panel", Block::new, stone(MapColor.COLOR_LIGHT_GRAY));
    public static final Block DARK_PANEL = register("dark_panel", Block::new, stone(MapColor.COLOR_GRAY));
    public static final Block MARBLE = register("marble", Block::new, stone(MapColor.SNOW));
    public static final Block BRICK_FACADE = register("brick_facade", Block::new, stone(MapColor.COLOR_RED));
    public static final Block METAL_CLADDING = register("metal_cladding", Block::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL));
    public static final Block ROOF_GRAVEL = register("roof_gravel", Block::new, stone(MapColor.STONE));
    public static final Block GARAGE_DOOR = register("garage_door", Block::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL));
    public static final Block SHOP_AWNING = register("shop_awning", Block::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).strength(0.8f).sound(SoundType.WOOL));
    public static final Block OFFICE_WINDOW = register("office_window", com.neoncoast.block.CityGlassBlock::new, glass());
    public static final Block GLASS_CURTAIN_WALL = register("glass_curtain_wall", com.neoncoast.block.CityGlassBlock::new, glass());
    public static final Block CROSSWALK = register("crosswalk", Block::new, stone(MapColor.COLOR_BLACK));
    public static final Block MANHOLE = register("manhole", Block::new, stone(MapColor.COLOR_BLACK));
    public static final String[] NEON_COLORS = {"red", "blue", "pink", "green", "yellow"};
    public static final Block[] NEON = new Block[NEON_COLORS.length];

    static {
        for (int i = 0; i < NEON_COLORS.length; i++) {
            NEON[i] = register("neon_" + NEON_COLORS[i], Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.5f).sound(SoundType.GLASS).lightLevel(st -> 15).instrument(NoteBlockInstrument.HAT));
        }
    }

    // ---- street props
    public static final Block VENDING_MACHINE = register("vending_machine", p -> new PropBlock(p, 1, 0, 2, 15, 16, 14), prop(MapColor.COLOR_RED).lightLevel(st -> 6));
    public static final Block DUMPSTER = register("dumpster", p -> new PropBlock(p, 0, 0, 2, 16, 12, 14), prop(MapColor.COLOR_GREEN));
    public static final Block AC_UNIT = register("ac_unit", p -> new PropBlock(p, 1, 0, 1, 15, 10, 15), prop(MapColor.METAL));
    public static final Block FIRE_HYDRANT = register("fire_hydrant", p -> new PropBlock(p, 5, 0, 5, 11, 11, 11), prop(MapColor.COLOR_RED));
    public static final Block TRAFFIC_LIGHT = register("traffic_light", p -> new PropBlock(p, 5, 0, 5, 11, 16, 11), prop(MapColor.COLOR_BLACK).lightLevel(st -> 5));
    public static final Block BENCH = register("bench", p -> new PropBlock(p, 0, 0, 4, 16, 8, 12), BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(1.5f).sound(SoundType.WOOD).noOcclusion());
    public static final Block TRASH_CAN = register("trash_can", p -> new PropBlock(p, 4, 0, 4, 12, 12, 12), prop(MapColor.COLOR_GRAY));

    private static BlockBehaviour.Properties prop(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(2.0f).sound(SoundType.METAL).noOcclusion();
    }

    private static BlockBehaviour.Properties glass() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6f)
                .sound(SoundType.GLASS).noOcclusion().isValidSpawn((st, w, p, t) -> false)
                .isRedstoneConductor((st, w, p) -> false).isSuffocating((st, w, p) -> false).isViewBlocking((st, w, p) -> false);
    }

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties stone(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(1.5f, 6.0f).sound(SoundType.STONE);
    }

    private static Block register(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        net.minecraft.resources.ResourceKey<Block> key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, NeonCoast.id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
        ModItems.register(name, p -> {
            BlockItem item = new BlockItem(block, p);
            item.registerBlocks(Item.BY_BLOCK, item);
            return item;
        }, new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    public static void init() {
    }
}
