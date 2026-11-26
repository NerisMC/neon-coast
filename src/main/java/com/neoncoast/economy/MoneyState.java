package com.neoncoast.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** Everyone's bank balance, saved with the world. */
public class MoneyState extends SavedData {
    public static final SavedData.Factory<MoneyState> TYPE = new SavedData.Factory<>(MoneyState::new, MoneyState::fromNbt, null);
    final Map<UUID, Long> balances = new HashMap<>();

    public static MoneyState fromNbt(CompoundTag nbt, HolderLookup.Provider lookup) {
        MoneyState state = new MoneyState();
        CompoundTag b = nbt.getCompound("Balances");
        for (String key : b.getAllKeys()) {
            state.balances.put(UUID.fromString(key), b.getLong(key));
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider lookup) {
        CompoundTag b = new CompoundTag();
        balances.forEach((uuid, amount) -> b.putLong(uuid.toString(), amount));
        nbt.put("Balances", b);
        return nbt;
    }
}
