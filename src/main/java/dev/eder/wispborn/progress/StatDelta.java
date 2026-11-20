package dev.eder.wispborn.progress;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StatDelta(int absorption, int efficiency, int reserve, int vitality) {
    public static final StatDelta ZERO = new StatDelta(0, 0, 0, 0);

    public static final StreamCodec<ByteBuf, StatDelta> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StatDelta::absorption,
            ByteBufCodecs.VAR_INT, StatDelta::efficiency,
            ByteBufCodecs.VAR_INT, StatDelta::reserve,
            ByteBufCodecs.VAR_INT, StatDelta::vitality,
            StatDelta::new
    );

    public int get(Stat stat) {
        return switch (stat) {
            case ABSORPTION -> this.absorption;
            case EFFICIENCY -> this.efficiency;
            case RESERVE -> this.reserve;
            case VITALITY -> this.vitality;
        };
    }

    public StatDelta with(Stat stat, int value) {
        return switch (stat) {
            case ABSORPTION -> new StatDelta(value, this.efficiency, this.reserve, this.vitality);
            case EFFICIENCY -> new StatDelta(this.absorption, value, this.reserve, this.vitality);
            case RESERVE -> new StatDelta(this.absorption, this.efficiency, value, this.vitality);
            case VITALITY -> new StatDelta(this.absorption, this.efficiency, this.reserve, value);
        };
    }

    public int total() {
        return this.absorption + this.efficiency + this.reserve + this.vitality;
    }
}
