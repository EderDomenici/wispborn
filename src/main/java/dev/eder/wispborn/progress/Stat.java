package dev.eder.wispborn.progress;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;
import java.util.function.IntFunction;

public enum Stat implements StringRepresentable {
    ABSORPTION("absorption"),
    EFFICIENCY("efficiency"),
    RESERVE("reserve"),
    VITALITY("vitality");

    public static final Codec<Stat> CODEC = StringRepresentable.fromEnum(Stat::values);
    private static final IntFunction<Stat> BY_ID =
            ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, Stat> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String serializedName;

    Stat(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String translationKey() {
        return "wispborn.stat." + this.serializedName;
    }

    public static Optional<Stat> byName(String name) {
        for (Stat stat : values()) {
            if (stat.serializedName.equalsIgnoreCase(name)) {
                return Optional.of(stat);
            }
        }
        return Optional.empty();
    }
}
