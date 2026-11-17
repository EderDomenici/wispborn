package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PossessStartPayload(int entityId) implements CustomPacketPayload {
    public static final Type<PossessStartPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "possess_start"));

    public static final StreamCodec<ByteBuf, PossessStartPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PossessStartPayload::entityId,
            PossessStartPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
