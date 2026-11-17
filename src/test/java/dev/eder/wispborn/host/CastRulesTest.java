package dev.eder.wispborn.host;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CastRulesTest {

    @Test
    void cancelsWhenNotWisp() {
        assertEquals(
                Optional.of(CancelReason.NOT_WISP),
                CastRules.cancelReason(false, true, 4.0D, 16.0D, true, false)
        );
    }

    @Test
    void cancelsWhenTargetGoneOrDead() {
        assertEquals(
                Optional.of(CancelReason.TARGET_GONE),
                CastRules.cancelReason(true, false, 4.0D, 16.0D, true, false)
        );
    }

    @Test
    void cancelsWhenOutOfRange() {
        assertEquals(
                Optional.of(CancelReason.OUT_OF_RANGE),
                CastRules.cancelReason(true, true, 16.1D, 16.0D, true, false)
        );
    }

    @Test
    void cancelsWhenNoLineOfSight() {
        assertEquals(
                Optional.of(CancelReason.NO_LINE_OF_SIGHT),
                CastRules.cancelReason(true, true, 4.0D, 16.0D, false, false)
        );
    }

    @Test
    void cancelsWhenTookDamage() {
        assertEquals(
                Optional.of(CancelReason.TOOK_DAMAGE),
                CastRules.cancelReason(true, true, 4.0D, 16.0D, true, true)
        );
    }

    @Test
    void returnsEmptyWhenAllConditionsRemainValid() {
        assertEquals(
                Optional.empty(),
                CastRules.cancelReason(true, true, 16.0D, 16.0D, true, false)
        );
    }
}
