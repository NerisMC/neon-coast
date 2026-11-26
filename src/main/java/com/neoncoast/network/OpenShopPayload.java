package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenShopPayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenShopPayload> ID = new CustomPacketPayload.Type<>(NeonCoast.id("open_shop"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, OpenShopPayload::pos, OpenShopPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
