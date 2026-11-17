package dev.eder.wispborn.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WispStats(int absorption, int efficiency, int reserve, int vitality) {
    public static final WispStats ZERO = new WispStats(0, 0, 0, 0);

    public static final Codec<WispStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("absorption").forGetter(WispStats::absorption),
            Codec.INT.fieldOf("efficiency").forGetter(WispStats::efficiency),
            Codec.INT.fieldOf("reserve").forGetter(WispStats::reserve),
            Codec.INT.fieldOf("vitality").forGetter(WispStats::vitality)
    ).apply(instance, WispStats::new));

    public int spent() {
        return this.absorption + this.efficiency + this.reserve + this.vitality;
    }

    public int get(Stat stat) {
        return switch (stat) {
            case ABSORPTION -> this.absorption;
            case EFFICIENCY -> this.efficiency;
            case RESERVE -> this.reserve;
            case VITALITY -> this.vitality;
        };
    }

    public WispStats with(Stat stat, int value) {
        return switch (stat) {
            case ABSORPTION -> new WispStats(value, this.efficiency, this.reserve, this.vitality);
            case EFFICIENCY -> new WispStats(this.absorption, value, this.reserve, this.vitality);
            case RESERVE -> new WispStats(this.absorption, this.efficiency, value, this.vitality);
            case VITALITY -> new WispStats(this.absorption, this.efficiency, this.reserve, value);
        };
    }
}
