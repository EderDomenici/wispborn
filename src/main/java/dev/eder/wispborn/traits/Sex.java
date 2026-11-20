package dev.eder.wispborn.traits;

import com.mojang.serialization.Codec;
import dev.eder.wispborn.WispbornMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.Optional;
import java.util.function.IntFunction;

public enum Sex implements StringRepresentable {
    MALE("male", "♂", "\uE003"),
    FEMALE("female", "♀", "\uE004");

    public static final ResourceLocation ICONS_FONT =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "icons");

    public static final Codec<Sex> CODEC = StringRepresentable.fromEnum(Sex::values);
    private static final IntFunction<Sex> BY_ID =
            ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, Sex> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String serializedName;
    private final String symbol;
    private final String glyphChar;

    Sex(String serializedName, String symbol, String glyphChar) {
        this.serializedName = serializedName;
        this.symbol = symbol;
        this.glyphChar = glyphChar;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String symbol() {
        return this.symbol;
    }

    public String glyphChar() {
        return this.glyphChar;
    }

    public Component glyphComponent() {
        return Component.literal(this.glyphChar)
                .withStyle(Style.EMPTY.withFont(ICONS_FONT).withColor(0xFFFFFF));
    }

    public String translationKey() {
        return "wispborn.sex." + this.serializedName;
    }

    public static Optional<Sex> byName(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        for (Sex sex : values()) {
            if (sex.serializedName.equals(normalized)) {
                return Optional.of(sex);
            }
        }
        return Optional.empty();
    }
}
