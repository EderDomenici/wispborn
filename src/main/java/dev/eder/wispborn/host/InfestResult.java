package dev.eder.wispborn.host;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.function.IntFunction;

public enum InfestResult {
    OK,
    NOT_A_MOB,
    HAS_PASSENGER,
    IMMUNE,
    TOO_STRONG,
    NO_MANA;

    private static final IntFunction<InfestResult> BY_ID =
            ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, InfestResult> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
}
