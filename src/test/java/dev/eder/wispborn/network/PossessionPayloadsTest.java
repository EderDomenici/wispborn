package dev.eder.wispborn.network;

import dev.eder.wispborn.host.InfestResult;
import dev.eder.wispborn.traits.Sex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PossessionPayloadsTest {

    @Test
    void possessStartPayloadStreamCodecRoundTrip() {
        PossessStartPayload original = new PossessStartPayload(42);
        ByteBuf buf = Unpooled.buffer();
        try {
            PossessStartPayload.STREAM_CODEC.encode(buf, original);
            PossessStartPayload decoded = PossessStartPayload.STREAM_CODEC.decode(buf);
            assertEquals(original, decoded);
        } finally {
            buf.release();
        }
    }

    @Test
    void castStatePayloadStreamCodecRoundTrip() {
        CastStatePayload original = new CastStatePayload(99, 20);
        ByteBuf buf = Unpooled.buffer();
        try {
            CastStatePayload.STREAM_CODEC.encode(buf, original);
            CastStatePayload decoded = CastStatePayload.STREAM_CODEC.decode(buf);
            assertEquals(original, decoded);
        } finally {
            buf.release();
        }
    }

    @Test
    void syncMobTraitsPayloadStreamCodecRoundTrip() {
        for (Sex sex : Sex.values()) {
            SyncMobTraitsPayload original = new SyncMobTraitsPayload(77, 12, sex);
            ByteBuf buf = Unpooled.buffer();
            try {
                SyncMobTraitsPayload.STREAM_CODEC.encode(buf, original);
                SyncMobTraitsPayload decoded = SyncMobTraitsPayload.STREAM_CODEC.decode(buf);
                assertEquals(original, decoded);
            } finally {
                buf.release();
            }
        }
    }

    @Test
    void targetInfoPayloadStreamCodecRoundTrip() {
        for (InfestResult result : InfestResult.values()) {
            TargetInfoPayload withTraits = new TargetInfoPayload(15, result, 6, Optional.of(Sex.FEMALE), 30, 4);
            ByteBuf buf = Unpooled.buffer();
            try {
                TargetInfoPayload.STREAM_CODEC.encode(buf, withTraits);
                assertEquals(withTraits, TargetInfoPayload.STREAM_CODEC.decode(buf));
            } finally {
                buf.release();
            }

            TargetInfoPayload immune = new TargetInfoPayload(16, InfestResult.IMMUNE, 0, Optional.empty(), 0, 4);
            ByteBuf bufImmune = Unpooled.buffer();
            try {
                TargetInfoPayload.STREAM_CODEC.encode(bufImmune, immune);
                assertEquals(immune, TargetInfoPayload.STREAM_CODEC.decode(bufImmune));
            } finally {
                bufImmune.release();
            }
        }
    }

    @Test
    void consumeRequestPayloadStreamCodecRoundTrip() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ConsumeRequestPayload.STREAM_CODEC.encode(buf, ConsumeRequestPayload.INSTANCE);
            ConsumeRequestPayload decoded = ConsumeRequestPayload.STREAM_CODEC.decode(buf);
            assertEquals(ConsumeRequestPayload.INSTANCE, decoded);
        } finally {
            buf.release();
        }
    }
}
