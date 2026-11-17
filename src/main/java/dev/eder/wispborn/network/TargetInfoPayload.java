package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.host.InfestResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TargetInfoPayload(int entityId, InfestResult result, int tier, int manaCost)
        implements CustomPacketPayload {
    public static final TargetInfoPayload NONE = new TargetInfoPayload(-1, InfestResult.NOT_A_MOB, 0, 0);

    public static final Type<TargetInfoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "target_info"));

    public static final StreamCodec<ByteBuf, TargetInfoPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TargetInfoPayload::entityId,
            InfestResult.STREAM_CODEC, TargetInfoPayload::result,
            ByteBufCodecs.VAR_INT, TargetInfoPayload::tier,
            ByteBufCodecs.VAR_INT, TargetInfoPayload::manaCost,
            TargetInfoPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
