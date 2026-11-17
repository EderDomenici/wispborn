package dev.eder.wispborn.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WispProgress(int totalEp, int level) {
    public static final WispProgress START = new WispProgress(0, 1);

    public static final Codec<WispProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("total_ep").forGetter(WispProgress::totalEp),
            Codec.INT.fieldOf("level").forGetter(WispProgress::level)
    ).apply(instance, WispProgress::new));
}
