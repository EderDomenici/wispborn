package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PossessCancelPayload() implements CustomPacketPayload {
    public static final PossessCancelPayload INSTANCE = new PossessCancelPayload();

    public static final Type<PossessCancelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "possess_cancel"));

    public static final StreamCodec<ByteBuf, PossessCancelPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
