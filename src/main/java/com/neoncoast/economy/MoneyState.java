package com.neoncoast.economy;

import com.mojang.serialization.Codec;
import com.neoncoast.NeonCoast;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Everyone's bank balance, saved with the world. */
public class MoneyState extends SavedData {
    private static final Codec<MoneyState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("Balances").codec()
            .xmap(MoneyState::fromMap, MoneyState::toMap);
    public static final SavedDataType<MoneyState> TYPE = new SavedDataType<>(NeonCoast.id("money"), MoneyState::new, CODEC, null);

    final Map<UUID, Long> balances = new HashMap<>();

    private static MoneyState fromMap(Map<String, Long> map) {
        MoneyState state = new MoneyState();
        map.forEach((key, amount) -> {
            try {
                state.balances.put(UUID.fromString(key), amount);
            } catch (IllegalArgumentException ignored) {
            }
        });
        return state;
    }

    private Map<String, Long> toMap() {
        Map<String, Long> map = new HashMap<>();
        balances.forEach((uuid, amount) -> map.put(uuid.toString(), amount));
        return map;
    }
}
