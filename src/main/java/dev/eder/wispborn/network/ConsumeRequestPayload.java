package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConsumeRequestPayload() implements CustomPacketPayload {
    public static final ConsumeRequestPayload INSTANCE = new ConsumeRequestPayload();

    public static final Type<ConsumeRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "consume_request"));

    public static final StreamCodec<ByteBuf, ConsumeRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
