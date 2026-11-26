package com.pipsparty.registry;

import com.pipsparty.PipsParty;
import com.pipsparty.block.CatalogBlock;
import com.pipsparty.block.ControlButtonBlock;
import com.pipsparty.block.HallLightBlock;
import com.pipsparty.block.PassableBlock;
import com.pipsparty.block.PropBlock;
import com.pipsparty.block.RegisterBlock;
import com.pipsparty.block.SecurityDoorBlock;
import com.pipsparty.block.TimeClockBlock;
import com.pipsparty.block.WallPropBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Every franchise block. Each gets a matching item. */
public final class ModBlocks {
    public static final List<Reg<Block>> ALL = new ArrayList<>();

    // ---- floors, walls, ceilings
    public static final Reg<Block> CHECKER_FLOOR = block("checker_floor", Block::new, solid(MapColor.SNOW, 1.5f));
    public static final Reg<Block> PARTY_WALL_RED = block("party_wall_red", Block::new, solid(MapColor.COLOR_RED, 1.5f));
    public static final Reg<Block> PARTY_WALL_BLUE = block("party_wall_blue", Block::new, solid(MapColor.COLOR_BLUE, 1.5f));
    public static final Reg<Block> PARTY_WALL_PURPLE = block("party_wall_purple", Block::new, solid(MapColor.COLOR_PURPLE, 1.5f));
    public static final Reg<Block> PARTY_WALL_TRIM = block("party_wall_trim", Block::new, solid(MapColor.COLOR_BLACK, 1.5f));
    public static final Reg<Block> CONFETTI_WALL = block("confetti_wall", Block::new, solid(MapColor.SNOW, 1.5f));
    public static final Reg<Block> CEILING_TILE = block("ceiling_tile", Block::new, solid(MapColor.WOOL, 1.0f));
    public static final Reg<Block> OFFICE_WALL = block("office_wall", Block::new, solid(MapColor.COLOR_GRAY, 2.0f));
    public static final Reg<Block> STAGE_FLOOR = block("stage_floor", Block::new, wood(MapColor.WOOD));
    public static final Reg<Block> STAGE_CURTAIN = block("stage_curtain", p -> new PassableBlock(p, false), cloth(MapColor.COLOR_RED));
    public static final Reg<Block> STAR_CURTAIN = block("star_curtain", p -> new PassableBlock(p, false), cloth(MapColor.COLOR_PURPLE));
    public static final Reg<Block> BALL_PIT = block("ball_pit", p -> new PassableBlock(p, true), cloth(MapColor.COLOR_YELLOW));

    // ---- furniture and attractions
    public static final Reg<Block> PARTY_TABLE = block("party_table", p -> new PropBlock(p, 0, 0, 0, 16, 12, 16), wood(MapColor.COLOR_RED).noOcclusion());
    public static final Reg<Block> PARTY_CHAIR = block("party_chair", p -> new PropBlock(p, 3, 0, 3, 13, 14, 13), wood(MapColor.COLOR_BLUE).noOcclusion());
    public static final Reg<Block> ARCADE_CABINET = block("arcade_cabinet", p -> new PropBlock(p, 1, 0, 1, 15, 16, 15), metal(MapColor.COLOR_BLACK).lightLevel(s -> 7));
    public static final Reg<Block> PRIZE_COUNTER = block("prize_counter", p -> new PropBlock(p, 0, 0, 0, 16, 16, 16), wood(MapColor.COLOR_YELLOW).lightLevel(s -> 5));
    public static final Reg<Block> TICKET_MACHINE = block("ticket_machine", p -> new PropBlock(p, 3, 0, 3, 13, 16, 13), metal(MapColor.COLOR_ORANGE).noOcclusion());
    public static final Reg<Block> PIZZA_OVEN = block("pizza_oven", p -> new PropBlock(p, 0, 0, 0, 16, 16, 16), stone(MapColor.STONE).lightLevel(s -> 10));
    public static final Reg<Block> KITCHEN_COUNTER = block("kitchen_counter", p -> new PropBlock(p, 0, 0, 0, 16, 16, 16), metal(MapColor.METAL));
    public static final Reg<Block> GIFT_BOX = block("gift_box", p -> new PropBlock(p, 2, 0, 2, 14, 13, 14), cloth(MapColor.COLOR_MAGENTA).noOcclusion());
    public static final Reg<Block> PARTY_BALLOONS = block("party_balloons", p -> new PassableBlock(p, false), cloth(MapColor.COLOR_RED).noOcclusion());
    public static final Reg<Block> SECURITY_DESK = block("security_desk", p -> new PropBlock(p, 0, 0, 0, 16, 13, 16), metal(MapColor.COLOR_GRAY).noOcclusion());
    public static final Reg<Block> DESK_FAN = block("desk_fan", p -> new PropBlock(p, 4, 0, 4, 12, 12, 12), metal(MapColor.COLOR_GRAY).noOcclusion());
    public static final Reg<Block> OFFICE_MONITOR = block("office_monitor", p -> new PropBlock(p, 2, 0, 3, 14, 12, 13), metal(MapColor.COLOR_BLACK).noOcclusion().lightLevel(s -> 6));

    // ---- posters and signs
    public static final Reg<Block> POSTER_PIP = block("poster_pip", WallPropBlock::new, paper());
    public static final Reg<Block> POSTER_RUSTY = block("poster_rusty", WallPropBlock::new, paper());
    public static final Reg<Block> POSTER_MARLA = block("poster_marla", WallPropBlock::new, paper());
    public static final Reg<Block> POSTER_GNASH = block("poster_gnash", WallPropBlock::new, paper());
    public static final Reg<Block> POSTER_RULES = block("poster_rules", WallPropBlock::new, paper());
    public static final Reg<Block> EMPLOYEES_ONLY_SIGN = block("employees_only_sign", WallPropBlock::new, paper());
    public static final Reg<Block> EXIT_SIGN = block("exit_sign", WallPropBlock::new, paper().lightLevel(s -> 9));

    // ---- security
    public static final Reg<Block> SECURITY_DOOR = block("security_door", SecurityDoorBlock::new,
            metal(MapColor.METAL).strength(5.0f, 1200.0f).noOcclusion());
    public static final Reg<Block> DOOR_BUTTON = block("door_button", p -> new ControlButtonBlock(p, true), metal(MapColor.COLOR_RED));
    public static final Reg<Block> LIGHT_BUTTON = block("light_button", p -> new ControlButtonBlock(p, false), metal(MapColor.COLOR_LIGHT_GRAY));
    public static final Reg<Block> HALL_LIGHT = block("hall_light", HallLightBlock::new, metal(MapColor.SNOW));
    public static final Reg<Block> TIME_CLOCK = block("time_clock", TimeClockBlock::new, metal(MapColor.COLOR_GRAY).noOcclusion());

    // ---- business
    public static final Reg<Block> PIZZERIA_REGISTER = block("pizzeria_register", RegisterBlock::new, metal(MapColor.COLOR_GREEN).noOcclusion());
    public static final Reg<Block> FRANCHISE_CATALOG = block("franchise_catalog", CatalogBlock::new, wood(MapColor.COLOR_PURPLE).noOcclusion());

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties solid(MapColor c, float strength) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(strength, 6.0f).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties stone(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(2.0f, 6.0f).sound(SoundType.STONE).noOcclusion();
    }

    private static BlockBehaviour.Properties wood(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(1.5f).sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties metal(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(2.5f, 6.0f).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties cloth(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(0.4f).sound(SoundType.WOOL).noCollision().noOcclusion();
    }

    private static BlockBehaviour.Properties paper() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.3f).sound(SoundType.WOOL).noCollision().noOcclusion();
    }

    private static Reg<Block> block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        Reg<Block> block = Reg.<Block>of(Registries.BLOCK, PipsParty.id(name), key -> factory.apply(props.setId(key)));
        Reg.<Item>of(Registries.ITEM, PipsParty.id(name), key -> {
            BlockItem item = new BlockItem(block.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix());
            item.registerBlocks(Item.BY_BLOCK, item);
            return item;
        });
        ALL.add(block);
        return block;
    }

    public static Supplier<Item> item(Reg<Block> block) {
        return () -> block.get().asItem();
    }

    public static void init() {
    }
}
