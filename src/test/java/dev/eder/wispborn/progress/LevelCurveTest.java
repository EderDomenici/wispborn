package dev.eder.wispborn.progress;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelCurveTest {
    private static final List<Integer> DEFAULT_THRESHOLDS =
            List.of(300, 800, 1600, 2800, 4500, 6800, 9800, 13600, 18300);

    @Test
    void levelForFollowsThresholds() {
        assertEquals(1, LevelCurve.levelFor(0, DEFAULT_THRESHOLDS));
        assertEquals(1, LevelCurve.levelFor(299, DEFAULT_THRESHOLDS));
        assertEquals(2, LevelCurve.levelFor(300, DEFAULT_THRESHOLDS));
        assertEquals(3, LevelCurve.levelFor(800, DEFAULT_THRESHOLDS));
        assertEquals(10, LevelCurve.levelFor(100_000, DEFAULT_THRESHOLDS));
    }

    @Test
    void nextThresholdReturnsNextOrEmptyAtMax() {
        assertEquals(OptionalInt.of(300), LevelCurve.nextThreshold(0, DEFAULT_THRESHOLDS));
        assertEquals(OptionalInt.of(300), LevelCurve.nextThreshold(299, DEFAULT_THRESHOLDS));
        assertEquals(OptionalInt.of(800), LevelCurve.nextThreshold(300, DEFAULT_THRESHOLDS));
        assertEquals(OptionalInt.empty(), LevelCurve.nextThreshold(18_300, DEFAULT_THRESHOLDS));
        assertEquals(OptionalInt.empty(), LevelCurve.nextThreshold(100_000, DEFAULT_THRESHOLDS));
    }
}
