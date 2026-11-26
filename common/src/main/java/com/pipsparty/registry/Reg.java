package com.pipsparty.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A registry entry that every loader can register at the right time: Fabric registers straight away,
 * Forge and NeoForge register during their RegisterEvent. Call {@link #get()} only after registration.
 */
public final class Reg<T> implements Supplier<T> {
    /** Everything the mod registers, in declaration order. Loaders walk this list. */
    public static final List<Reg<?>> ALL = new ArrayList<>();

    public final ResourceKey<? extends Registry<? super T>> registry;
    public final Identifier id;
    private final Function<ResourceKey<T>, ? extends T> factory;
    private T value;

    private Reg(ResourceKey<? extends Registry<? super T>> registry, Identifier id, Function<ResourceKey<T>, ? extends T> factory) {
        this.registry = registry;
        this.id = id;
        this.factory = factory;
    }

    public static <T> Reg<T> of(ResourceKey<? extends Registry<? super T>> registry, Identifier id, Function<ResourceKey<T>, ? extends T> factory) {
        Reg<T> r = new Reg<>(registry, id, factory);
        ALL.add(r);
        return r;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ResourceKey<T> key() {
        return (ResourceKey<T>) ResourceKey.create((ResourceKey) registry, id);
    }

    /** Creates the object (once). Loaders call this from their registration hook and register the result. */
    public T create() {
        if (value == null) value = factory.apply(key());
        return value;
    }

    @Override
    public T get() {
        if (value == null) throw new IllegalStateException("Not registered yet: " + id);
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }
}
