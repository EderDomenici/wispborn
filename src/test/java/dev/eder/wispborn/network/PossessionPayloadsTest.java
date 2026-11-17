package dev.eder.wispborn.network;

import dev.eder.wispborn.host.InfestResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

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
    void targetInfoPayloadStreamCodecRoundTrip() {
        for (InfestResult result : InfestResult.values()) {
            TargetInfoPayload original = new TargetInfoPayload(15, result, 2, 30);
            ByteBuf buf = Unpooled.buffer();
            try {
                TargetInfoPayload.STREAM_CODEC.encode(buf, original);
                TargetInfoPayload decoded = TargetInfoPayload.STREAM_CODEC.decode(buf);
                assertEquals(original, decoded);
            } finally {
                buf.release();
            }
        }
    }
}
