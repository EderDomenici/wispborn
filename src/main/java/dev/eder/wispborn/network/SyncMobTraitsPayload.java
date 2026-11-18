package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.traits.Sex;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncMobTraitsPayload(int entityId, int level, Sex sex) implements CustomPacketPayload {
    public static final Type<SyncMobTraitsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "sync_mob_traits"));

    public static final StreamCodec<ByteBuf, SyncMobTraitsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncMobTraitsPayload::entityId,
            ByteBufCodecs.VAR_INT, SyncMobTraitsPayload::level,
            Sex.STREAM_CODEC, SyncMobTraitsPayload::sex,
            SyncMobTraitsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
