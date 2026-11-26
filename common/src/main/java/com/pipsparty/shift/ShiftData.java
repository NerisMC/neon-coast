package com.pipsparty.shift;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipsparty.PipsParty;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Saved with the world: which night each player is on, and when each register was last emptied. */
public class ShiftData extends SavedData {
    private static final Codec<ShiftData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("Nights").forGetter(d -> d.nights),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("Registers").forGetter(d -> d.registers)
    ).apply(i, ShiftData::new));
    public static final SavedDataType<ShiftData> TYPE = new SavedDataType<>(PipsParty.id("shifts"), ShiftData::new, CODEC, null);

    private final Map<String, Integer> nights;
    private final Map<String, Long> registers;

    public ShiftData() {
        this(new HashMap<>(), new HashMap<>());
    }

    private ShiftData(Map<String, Integer> nights, Map<String, Long> registers) {
        this.nights = new HashMap<>(nights);
        this.registers = new HashMap<>(registers);
    }

    public static ShiftData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int night(UUID player) {
        return nights.getOrDefault(player.toString(), 1);
    }

    public void setNight(UUID player, int night) {
        nights.put(player.toString(), night);
        setDirty();
    }

    public long lastCollected(String key, long fallback) {
        return registers.getOrDefault(key, fallback);
    }

    public void setLastCollected(String key, long day) {
        registers.put(key, day);
        setDirty();
    }
}
