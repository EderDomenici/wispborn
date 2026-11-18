package dev.eder.wispborn.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record FormState(FormType type, Optional<ResourceLocation> hostEntity) {
    public static final FormState WISP = new FormState(FormType.WISP, Optional.empty());

    public static FormState host(ResourceLocation entityTypeId) {
        return new FormState(FormType.HOST, Optional.of(entityTypeId));
    }

    public static final Codec<FormState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FormType.CODEC.fieldOf("type").forGetter(FormState::type),
            ResourceLocation.CODEC.optionalFieldOf("host_entity").forGetter(FormState::hostEntity)
    ).apply(instance, FormState::new));

    public static final StreamCodec<ByteBuf, FormState> STREAM_CODEC = StreamCodec.composite(
            FormType.STREAM_CODEC, FormState::type,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), FormState::hostEntity,
            FormState::new
    );
}
