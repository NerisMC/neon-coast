package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record JobSyncPayload(boolean active, BlockPos target) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<JobSyncPayload> ID = new CustomPacketPayload.Type<>(NeonCoast.id("job"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JobSyncPayload> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, JobSyncPayload::active, BlockPos.STREAM_CODEC, JobSyncPayload::target, JobSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
