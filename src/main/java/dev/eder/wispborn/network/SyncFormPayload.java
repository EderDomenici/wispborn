package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncFormPayload(int entityId, FormState state) implements CustomPacketPayload {
    public static final Type<SyncFormPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "sync_form"));

    public static final StreamCodec<ByteBuf, SyncFormPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncFormPayload::entityId,
            FormState.STREAM_CODEC, SyncFormPayload::state,
            SyncFormPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
