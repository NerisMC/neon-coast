package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WantedSyncPayload(int stars, boolean evading) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WantedSyncPayload> ID = new CustomPacketPayload.Type<>(NeonCoast.id("wanted"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WantedSyncPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, WantedSyncPayload::stars, ByteBufCodecs.BOOL, WantedSyncPayload::evading, WantedSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
