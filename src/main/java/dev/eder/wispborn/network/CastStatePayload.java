package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CastStatePayload(int targetEntityId, int durationTicks) implements CustomPacketPayload {
    public static final CastStatePayload NONE = new CastStatePayload(-1, 0);

    public static final Type<CastStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "cast_state"));

    public static final StreamCodec<ByteBuf, CastStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CastStatePayload::targetEntityId,
            ByteBufCodecs.VAR_INT, CastStatePayload::durationTicks,
            CastStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
