package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.host.InfestResult;
import dev.eder.wispborn.traits.Sex;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record TargetInfoPayload(
        int entityId,
        InfestResult result,
        int mobLevel,
        Optional<Sex> sex,
        int manaCost,
        int wispLevel
) implements CustomPacketPayload {
    public static final TargetInfoPayload NONE =
            new TargetInfoPayload(-1, InfestResult.NOT_A_MOB, 0, Optional.empty(), 0, 1);

    public static final Type<TargetInfoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "target_info"));

    public static final StreamCodec<ByteBuf, TargetInfoPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TargetInfoPayload::entityId,
            InfestResult.STREAM_CODEC, TargetInfoPayload::result,
            ByteBufCodecs.VAR_INT, TargetInfoPayload::mobLevel,
            ByteBufCodecs.optional(Sex.STREAM_CODEC), TargetInfoPayload::sex,
            ByteBufCodecs.VAR_INT, TargetInfoPayload::manaCost,
            ByteBufCodecs.VAR_INT, TargetInfoPayload::wispLevel,
            TargetInfoPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
