package dev.eder.wispborn.traits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MobTraits(int level, Sex sex) {
    public static final Codec<MobTraits> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("level").forGetter(MobTraits::level),
            Sex.CODEC.fieldOf("sex").forGetter(MobTraits::sex)
    ).apply(instance, MobTraits::new));

    public static final StreamCodec<ByteBuf, MobTraits> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobTraits::level,
            Sex.STREAM_CODEC, MobTraits::sex,
            MobTraits::new
    );
}
