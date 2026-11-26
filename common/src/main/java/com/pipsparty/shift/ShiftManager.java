package com.pipsparty.shift;

import com.pipsparty.block.HallLightBlock;
import com.pipsparty.block.SecurityDoorBlock;
import com.pipsparty.entity.AnimatronicEntity;
import com.pipsparty.entity.Character;
import com.pipsparty.entity.SecurityCameraEntity;
import com.pipsparty.item.CameraTabletItem;
import com.pipsparty.registry.ModItems;
import com.pipsparty.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Night shifts. Punch in at a Time Clock and survive 12 AM to 6 AM (45 seconds an hour).
 *
 * The animatronics near the clock stop free-roaming: every 5 seconds each one rolls against its AI level
 * (night + time + how busy your pizzeria is) and, on a hit, moves one room closer: from its stage spot,
 * through the rooms your Security Cameras watch (farthest first), to the doorway outside one of the office's
 * Security Doors. From the doorway it tries to get in: a closed door sends it back, an open one is game over.
 *
 * Doors, hall lights and the camera tablet all use power. At 0% the doors open, the lights die, and Pip
 * comes with his music box. Gnash watches you instead: leave him unchecked on the cameras and he sprints
 * down the west hall and hammers the door (that costs power), or worse.
 */
public final class ShiftManager {
    public static final int TICKS_PER_HOUR = 900;
    private static final float DRAIN_PER_TICK = 0.0052f;   // base usage alone costs ~28% a night
    private static final Map<String, Shift> SHIFTS = new HashMap<>();

    private ShiftManager() {
    }

    private static final class Bot {
        final UUID id;
        final Character character;
        int progress;         // 0 = stage, 1..rooms = rooms, rooms+1 = at the door
        int door;
        int runTicks;         // Gnash's sprint timer
        Bot(UUID id, Character character) {
            this.id = id;
            this.character = character;
        }
    }

    private static final class Shift {
        ServerLevel level;
        BlockPos clock;
        UUID guard;
        int night;
        int ticks;
        float power = 100.0f;
        int usage = 1;
        ServerBossEvent bar;
        final List<Bot> bots = new ArrayList<>();
        final List<BlockPos> rooms = new ArrayList<>();
        final List<BlockPos> doorSpots = new ArrayList<>();
        final List<List<BlockPos>> doorBlocks = new ArrayList<>();
        int bonus;
        boolean powerOut;
        int powerOutTicks;
        int pipStrike = -1;
        int gnashMeter;
        int awayTicks;
    }

    private static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().identifier() + "|" + pos.asLong();
    }

    // ------------------------------------------------------------------ queries used by blocks and entities

    /** Door and light buttons stop working once the power's out. */
    public static boolean doorsWork(net.minecraft.world.level.Level level, BlockPos pos) {
        for (Shift s : SHIFTS.values()) {
            if (s.level == level && s.powerOut && s.clock.closerThan(pos, 14.0)) return false;
        }
        return true;
    }

    public static boolean isGuardOnShift(ServerPlayer p) {
        for (Shift s : SHIFTS.values()) if (s.guard.equals(p.getUUID())) return true;
        return false;
    }

    // ------------------------------------------------------------------ start

    public static void clockIn(ServerLevel level, BlockPos clock, ServerPlayer player) {
        String k = key(level, clock);
        Shift existing = SHIFTS.get(k);
        if (existing != null) {
            if (existing.guard.equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("You're on the clock. Survive until 6 AM.").withStyle(ChatFormatting.YELLOW));
            } else {
                player.sendSystemMessage(Component.literal("Someone else is working this shift. Grab an Override Chip and go hunt them...")
                        .withStyle(ChatFormatting.RED));
            }
            return;
        }
        for (Shift s : SHIFTS.values()) {
            if (s.guard.equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("You're already on a shift somewhere else."));
                return;
            }
        }
        Shift s = new Shift();
        s.level = level;
        s.clock = clock.immutable();
        s.guard = player.getUUID();
        s.night = ShiftData.get(level.getServer()).night(player.getUUID());

        // the office doors: clusters of Security Door blocks within 10 blocks of the clock
        List<BlockPos> doors = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(clock.offset(-10, -3, -10), clock.offset(10, 3, 10))) {
            if (level.getBlockState(p).getBlock() instanceof SecurityDoorBlock) doors.add(p.immutable());
        }
        for (BlockPos d : doors) {
            List<BlockPos> group = null;
            for (List<BlockPos> g : s.doorBlocks) {
                if (g.get(0).closerThan(d, 2.5)) group = g;
            }
            if (group == null) {
                group = new ArrayList<>();
                s.doorBlocks.add(group);
            }
            group.add(d);
        }
        s.doorBlocks.sort(Comparator.comparingInt(g -> g.get(0).getX() + g.get(0).getZ() * 3));   // west door first
        for (List<BlockPos> g : s.doorBlocks) {
            BlockPos bottom = g.stream().min(Comparator.comparingInt(BlockPos::getY)).orElse(g.get(0));
            int dx = bottom.getX() - clock.getX(), dz = bottom.getZ() - clock.getZ();
            Direction out = Math.abs(dx) >= Math.abs(dz) ? (dx < 0 ? Direction.WEST : Direction.EAST) : (dz < 0 ? Direction.NORTH : Direction.SOUTH);
            s.doorSpots.add(bottom.relative(out));
        }
        if (s.doorSpots.isEmpty()) {
            player.sendSystemMessage(Component.literal("This office has no Security Doors within 10 blocks of the Time Clock. Good luck...")
                    .withStyle(ChatFormatting.RED));
        }

        // the rooms: what the cameras watch, farthest from the office first
        AABB area = new AABB(clock).inflate(48.0);
        List<SecurityCameraEntity> cams = level.getEntitiesOfClass(SecurityCameraEntity.class, area, c -> true);
        for (SecurityCameraEntity c : cams) s.rooms.add(c.roomSpot());
        s.rooms.sort(Comparator.comparingDouble(r -> -r.distSqr(clock)));

        // the band
        for (AnimatronicEntity a : level.getEntitiesOfClass(AnimatronicEntity.class, area, a -> !a.isVehicle())) {
            a.setShiftControlled(true);
            a.returnHome();
            Bot b = new Bot(a.getUUID(), a.character);
            b.door = a.character.door >= 0 ? a.character.door : level.getRandom().nextInt(2);
            s.bots.add(b);
        }
        s.bonus = Math.min(4, Economy.funRating(level, clock) / 25);

        s.bar = new ServerBossEvent(UUID.randomUUID(), Component.literal("Night " + s.night), BossEvent.BossBarColor.YELLOW,
                BossEvent.BossBarOverlay.NOTCHED_6);
        s.bar.addPlayer(player);
        SHIFTS.put(k, s);
        title(player, "Night " + s.night, "12 AM");
        player.sendSystemMessage(Component.literal("Night " + s.night + " at Pip's Party Palace. " + s.bots.size() + " animatronics, "
                + cams.size() + " cameras, " + s.doorSpots.size() + " doors. Watch your power.").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (s.night == 1) {
            player.sendSystemMessage(Component.literal("Tip: buttons slam the doors and flick the hall lights. Hold the Camera Tablet to "
                    + "watch the cameras (A/D to switch). Everything uses power.").withStyle(ChatFormatting.GRAY));
        }
        level.scheduleTick(clock, level.getBlockState(clock).getBlock(), 1);
    }

    // ------------------------------------------------------------------ tick (scheduled by the Time Clock block)

    public static void tick(ServerLevel level, BlockPos clock) {
        Shift s = SHIFTS.get(key(level, clock));
        if (s == null) return;
        level.scheduleTick(clock, level.getBlockState(clock).getBlock(), 1);
        ServerPlayer guard = level.getServer().getPlayerList().getPlayer(s.guard);
        if (guard == null || !guard.isAlive() || guard.level() != level) {
            end(s, false, "The guard is gone. Shift over.");
            return;
        }
        if (guard.blockPosition().distSqr(clock) > 16 * 16) {
            if (++s.awayTicks == 1) guard.sendSystemMessage(Component.literal("Get back to the office!").withStyle(ChatFormatting.RED));
            if (s.awayTicks > 200) {
                end(s, false, "You walked off the job. Shift failed.");
                return;
            }
        } else {
            s.awayTicks = 0;
        }

        s.ticks++;
        int hour = s.ticks / TICKS_PER_HOUR;
        if (hour >= 6) {
            win(s, guard);
            return;
        }

        // power
        if (s.ticks % 20 == 0) s.usage = countUsage(s, guard);
        if (!s.powerOut) {
            s.power -= s.usage * DRAIN_PER_TICK * (CameraTabletItem.isWatching(guard) ? 1.25f : 1.0f);
            if (s.power <= 0.0f) powerOut(s);
        } else {
            tickPowerOut(s, guard);
        }

        // the animatronics
        if (!s.powerOut && s.ticks % 100 == 0) moveOpportunity(s, guard, hour);
        tickGnashRun(s, guard);

        if (s.ticks % 10 == 0) updateBar(s, hour);
    }

    private static int countUsage(Shift s, ServerPlayer guard) {
        int usage = 1;
        for (List<BlockPos> g : s.doorBlocks) {
            BlockState st = s.level.getBlockState(g.get(0));
            if (st.getBlock() instanceof SecurityDoorBlock && st.getValue(SecurityDoorBlock.CLOSED)) usage++;
        }
        for (BlockPos p : BlockPos.betweenClosed(s.clock.offset(-12, -2, -12), s.clock.offset(12, 6, 12))) {
            BlockState st = s.level.getBlockState(p);
            if (st.getBlock() instanceof HallLightBlock && st.getValue(HallLightBlock.LIT)) usage++;
        }
        if (CameraTabletItem.isWatching(guard)) usage++;
        return usage;
    }

    private static void updateBar(Shift s, int hour) {
        String time = hour == 0 ? "12 AM" : hour + " AM";
        StringBuilder bars = new StringBuilder();
        for (int i = 0; i < 5; i++) bars.append(i < s.usage ? '▮' : '▯');
        int pct = Math.max(0, (int) Math.ceil(s.power));
        s.bar.setName(Component.literal("Night " + s.night + "   " + time + "   Power " + pct + "%   Usage " + bars));
        s.bar.setProgress(Math.min(1.0f, s.ticks / (float) (TICKS_PER_HOUR * 6)));
        s.bar.setColor(s.powerOut ? BossEvent.BossBarColor.RED : pct < 20 ? BossEvent.BossBarColor.RED : pct < 50
                ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN);
        if (s.ticks % TICKS_PER_HOUR == 0 && hour > 0) {
            ServerPlayer guard = s.level.getServer().getPlayerList().getPlayer(s.guard);
            if (guard != null) guard.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(time)));
        }
    }

    // ------------------------------------------------------------------ AI

    private static AnimatronicEntity bot(Shift s, Bot b) {
        Entity e = s.level.getEntity(b.id);
        return e instanceof AnimatronicEntity a && a.isAlive() ? a : null;
    }

    private static boolean doorClosed(Shift s, int door) {
        if (door >= s.doorBlocks.size()) return false;
        BlockState st = s.level.getBlockState(s.doorBlocks.get(door).get(0));
        return st.getBlock() instanceof SecurityDoorBlock && st.getValue(SecurityDoorBlock.CLOSED);
    }

    private static void moveOpportunity(Shift s, ServerPlayer guard, int hour) {
        RandomSource r = s.level.getRandom();
        boolean watching = CameraTabletItem.isWatching(guard);
        for (Bot b : s.bots) {
            AnimatronicEntity a = bot(s, b);
            if (a == null || a.isVehicle() || a.getScareTarget() >= 0 || b.runTicks > 0) continue;
            int level = Math.min(20, b.character.aiLevel(s.night, hour) + s.bonus);
            if (level <= 0 || r.nextInt(20) + 1 > level) continue;

            if (b.character == Character.GNASH) {
                // Gnash creeps out of the grotto while you're not watching the cameras
                if (watching) {
                    s.gnashMeter = Math.max(0, s.gnashMeter - 1);
                } else if (++s.gnashMeter >= 4 && !s.doorSpots.isEmpty()) {
                    BlockPos spot = s.doorSpots.get(0);
                    a.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 2.3);
                    b.runTicks = 140;
                    s.level.playSound(null, a.getX(), a.getY(), a.getZ(), ModSounds.GATOR_RUN.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
                }
                continue;
            }
            if (b.character == Character.PIP && (watching || (s.night < 3 && hour < 3))) continue;   // Pip moves while your eyes are off the cameras

            int atDoor = s.rooms.size() + 1;
            if (b.progress >= atDoor && !s.doorSpots.isEmpty()) {
                int door = Math.min(b.door, s.doorSpots.size() - 1);
                if (doorClosed(s, door)) {
                    s.level.playSound(null, s.doorSpots.get(door), ModSounds.DOOR_BANG.get(), SoundSource.HOSTILE, 1.4f, 0.9f);
                    b.progress = Math.max(1, r.nextInt(Math.max(1, s.rooms.size())) + 1);
                    teleport(a, s.rooms.isEmpty() ? a.getHome() : s.rooms.get(b.progress - 1));
                    if (b.character.door < 0) b.door = r.nextInt(2);
                } else {
                    a.jumpscare(guard, true);
                }
            } else {
                b.progress++;
                BlockPos to = b.progress <= s.rooms.size() ? s.rooms.get(b.progress - 1)
                        : s.doorSpots.isEmpty() ? null : s.doorSpots.get(Math.min(b.door, s.doorSpots.size() - 1));
                if (to != null) teleport(a, to);
                if (b.progress >= atDoor && r.nextInt(3) == 0) {
                    s.level.playSound(null, a.getX(), a.getY(), a.getZ(), ModSounds.GIGGLE.get(), SoundSource.HOSTILE, 0.8f, 0.8f + r.nextFloat() * 0.3f);
                }
            }
        }
    }

    private static void teleport(AnimatronicEntity a, BlockPos to) {
        if (to == null) return;
        a.getNavigation().stop();
        a.snapTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, a.getRandom().nextFloat() * 360.0f, 0.0f);
        a.level().playSound(null, to, ModSounds.SERVO_STEP.get(), SoundSource.HOSTILE, 0.6f, 0.7f);
    }

    private static void tickGnashRun(Shift s, ServerPlayer guard) {
        for (Bot b : s.bots) {
            if (b.runTicks <= 0) continue;
            AnimatronicEntity a = bot(s, b);
            if (a == null) {
                b.runTicks = 0;
                continue;
            }
            BlockPos spot = s.doorSpots.get(0);
            boolean arrived = a.blockPosition().closerThan(spot, 2.0);
            if (--b.runTicks == 0 || arrived) {
                b.runTicks = 0;
                s.gnashMeter = 0;
                if (doorClosed(s, 0)) {
                    for (int i = 0; i < 3; i++) {
                        s.level.playSound(null, spot, ModSounds.DOOR_BANG.get(), SoundSource.HOSTILE, 1.6f, 0.8f + i * 0.1f);
                    }
                    s.power -= 6.0f;
                    a.returnHome();
                } else {
                    a.jumpscare(guard, true);
                }
            }
        }
    }

    // ------------------------------------------------------------------ power out

    private static void powerOut(Shift s) {
        s.powerOut = true;
        s.power = 0.0f;
        s.powerOutTicks = 0;
        for (List<BlockPos> g : s.doorBlocks) {
            for (BlockPos p : g) {
                BlockState st = s.level.getBlockState(p);
                if (st.getBlock() instanceof SecurityDoorBlock) s.level.setBlock(p, st.setValue(SecurityDoorBlock.CLOSED, false), Block.UPDATE_ALL);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(s.clock.offset(-12, -2, -12), s.clock.offset(12, 6, 12))) {
            BlockState st = s.level.getBlockState(p);
            if (st.getBlock() instanceof HallLightBlock && st.getValue(HallLightBlock.LIT)) {
                s.level.setBlock(p, st.setValue(HallLightBlock.LIT, false), Block.UPDATE_ALL);
            }
        }
        s.level.playSound(null, s.clock, ModSounds.POWER_DOWN.get(), SoundSource.BLOCKS, 2.0f, 1.0f);
        s.pipStrike = 220 + s.level.getRandom().nextInt(260);
    }

    private static void tickPowerOut(Shift s, ServerPlayer guard) {
        s.powerOutTicks++;
        AnimatronicEntity pip = null;
        for (Bot b : s.bots) {
            if (b.character == Character.PIP) pip = bot(s, b);
        }
        if (pip == null && !s.bots.isEmpty()) pip = bot(s, s.bots.get(0));
        if (pip == null) return;
        if (s.powerOutTicks == 60 && !s.doorSpots.isEmpty()) {
            teleport(pip, s.doorSpots.get(0));
            s.level.playSound(null, s.doorSpots.get(0), ModSounds.MUSIC_BOX.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
        }
        if (s.powerOutTicks == s.pipStrike) pip.jumpscare(guard, true);
    }

    // ------------------------------------------------------------------ end

    private static void win(Shift s, ServerPlayer guard) {
        s.level.playSound(null, guard.getX(), guard.getY(), guard.getZ(), ModSounds.CHIME_6AM.get(), SoundSource.MASTER, 1.5f, 1.0f);
        title(guard, "6 AM", "Night " + s.night + " complete!");
        int pay = 10 + s.night * 15;
        ItemStack coins = new ItemStack(ModItems.PIP_COIN.get(), pay);
        if (!guard.getInventory().add(coins)) guard.drop(coins, false);
        ShiftData.get(s.level.getServer()).setNight(s.guard, s.night + 1);
        guard.sendSystemMessage(Component.literal("Paycheck: " + pay + " Pip Coins. "
                + (s.night >= 5 ? "You survived the week! Nights keep getting harder from here..." : "Night " + (s.night + 1) + " is next."))
                .withStyle(ChatFormatting.GOLD));
        end(s, true, null);
    }

    /** Called by an animatronic after a killing jumpscare. */
    public static void onGuardCaught(ServerLevel level, ServerPlayer player, AnimatronicEntity by) {
        for (Shift s : new ArrayList<>(SHIFTS.values())) {
            if (s.guard.equals(player.getUUID())) {
                title(player, "GAME OVER", "Caught by " + by.character.displayName + " on Night " + s.night);
                end(s, false, null);
            }
        }
    }

    private static void end(Shift s, boolean won, String message) {
        s.bar.removeAllPlayers();
        for (Bot b : s.bots) {
            AnimatronicEntity a = bot(s, b);
            if (a != null) {
                a.setShiftControlled(false);
                a.returnHome();
            }
        }
        for (List<BlockPos> g : s.doorBlocks) {
            for (BlockPos p : g) {
                BlockState st = s.level.getBlockState(p);
                if (st.getBlock() instanceof SecurityDoorBlock && st.getValue(SecurityDoorBlock.CLOSED)) {
                    s.level.setBlock(p, st.setValue(SecurityDoorBlock.CLOSED, false), Block.UPDATE_ALL);
                }
            }
        }
        if (message != null) {
            ServerPlayer guard = s.level.getServer().getPlayerList().getPlayer(s.guard);
            if (guard != null) guard.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
        }
        for (Iterator<Map.Entry<String, Shift>> it = SHIFTS.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue() == s) it.remove();
        }
    }

    private static void title(ServerPlayer p, String title, String subtitle) {
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
    }
}
