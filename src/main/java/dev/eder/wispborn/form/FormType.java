package dev.eder.wispborn.form;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum FormType implements StringRepresentable {
    HUMAN("human"),
    WISP("wisp"),
    HOST("host");

    public static final Codec<FormType> CODEC = StringRepresentable.fromEnum(FormType::values);
    private static final IntFunction<FormType> BY_ID =
            ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, FormType> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String serializedName;

    FormType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
