package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MoneySyncPayload(long money) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MoneySyncPayload> ID = new CustomPacketPayload.Type<>(NeonCoast.id("money"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MoneySyncPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, MoneySyncPayload::money, MoneySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
