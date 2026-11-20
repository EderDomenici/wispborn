package dev.eder.wispborn.network;

import dev.eder.wispborn.WispbornMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncWispStatePayload(
        int level,
        int totalEp,
        int levelStartEp,
        int nextLevelEp,
        int mana,
        int maxMana,
        int availablePoints,
        boolean hostConsumeReady,
        float hostEpFactor
) implements CustomPacketPayload {
    public static final SyncWispStatePayload DEFAULT =
            new SyncWispStatePayload(1, 0, 0, 300, 50, 50, 0, false, 1.0F);

    public static final Type<SyncWispStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "sync_wisp_state"));

    public static final StreamCodec<ByteBuf, SyncWispStatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncWispStatePayload decode(ByteBuf buf) {
            int level = ByteBufCodecs.VAR_INT.decode(buf);
            int totalEp = ByteBufCodecs.VAR_INT.decode(buf);
            int levelStartEp = ByteBufCodecs.VAR_INT.decode(buf);
            int nextLevelEp = ByteBufCodecs.VAR_INT.decode(buf);
            int mana = ByteBufCodecs.VAR_INT.decode(buf);
            int maxMana = ByteBufCodecs.VAR_INT.decode(buf);
            int availablePoints = ByteBufCodecs.VAR_INT.decode(buf);
            boolean hostConsumeReady = ByteBufCodecs.BOOL.decode(buf);
            float hostEpFactor = ByteBufCodecs.FLOAT.decode(buf);
            return new SyncWispStatePayload(
                    level,
                    totalEp,
                    levelStartEp,
                    nextLevelEp,
                    mana,
                    maxMana,
                    availablePoints,
                    hostConsumeReady,
                    hostEpFactor
            );
        }

        @Override
        public void encode(ByteBuf buf, SyncWispStatePayload value) {
            ByteBufCodecs.VAR_INT.encode(buf, value.level());
            ByteBufCodecs.VAR_INT.encode(buf, value.totalEp());
            ByteBufCodecs.VAR_INT.encode(buf, value.levelStartEp());
            ByteBufCodecs.VAR_INT.encode(buf, value.nextLevelEp());
            ByteBufCodecs.VAR_INT.encode(buf, value.mana());
            ByteBufCodecs.VAR_INT.encode(buf, value.maxMana());
            ByteBufCodecs.VAR_INT.encode(buf, value.availablePoints());
            ByteBufCodecs.BOOL.encode(buf, value.hostConsumeReady());
            ByteBufCodecs.FLOAT.encode(buf, value.hostEpFactor());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
