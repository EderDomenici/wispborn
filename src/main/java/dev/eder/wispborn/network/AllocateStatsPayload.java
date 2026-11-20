package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.progress.StatDelta;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AllocateStatsPayload(StatDelta delta) implements CustomPacketPayload {
    public static final Type<AllocateStatsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "allocate_stats"));

    public static final StreamCodec<ByteBuf, AllocateStatsPayload> STREAM_CODEC = StreamCodec.composite(
            StatDelta.STREAM_CODEC, AllocateStatsPayload::delta,
            AllocateStatsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
