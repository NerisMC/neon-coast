package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CarCrashPayload(float impact) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CarCrashPayload> ID = new CustomPacketPayload.Type<>(NeonCoast.id("car_crash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CarCrashPayload> CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, CarCrashPayload::impact, CarCrashPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
