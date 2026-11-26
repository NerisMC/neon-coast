package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BuyPayload(int index, BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BuyPayload> ID = new CustomPacketPayload.Type<>(NeonCoast.id("buy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuyPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, BuyPayload::index, BlockPos.STREAM_CODEC, BuyPayload::pos, BuyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
