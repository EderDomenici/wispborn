package dev.eder.wispborn.traits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MobLevelRange(int min, int max) {
    private static final Codec<MobLevelRange> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("min").forGetter(MobLevelRange::min),
            Codec.INT.fieldOf("max").forGetter(MobLevelRange::max)
    ).apply(instance, MobLevelRange::new));

    public static final Codec<MobLevelRange> CODEC = RAW_CODEC.validate(range -> {
        if (range.min() < 1) {
            return DataResult.error(() -> "min must be >= 1, got " + range.min());
        }
        if (range.max() < range.min()) {
            return DataResult.error(() -> "max (" + range.max() + ") must be >= min (" + range.min() + ")");
        }
        return DataResult.success(range);
    });
}
