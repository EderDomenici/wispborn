package dev.eder.wispborn.progress;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelCurveTest {
    private static final List<Integer> DEFAULT_THRESHOLDS = List.of(
            300, 800, 1600, 2800, 4500, 6800, 9800, 13600, 18300,
            24000, 30800, 38800, 48100, 58800, 71000, 84800, 100300, 117600, 136800
    );

    @Test
    void levelForFollowsThresholdsUpToTwenty() {
        assertEquals(1, LevelCurve.levelFor(0, DEFAULT_THRESHOLDS));
        assertEquals(1, LevelCurve.levelFor(299, DEFAULT_THRESHOLDS));
        assertEquals(2, LevelCurve.levelFor(300, DEFAULT_THRESHOLDS));
        assertEquals(3, LevelCurve.levelFor(800, DEFAULT_THRESHOLDS));
        assertEquals(19, LevelCurve.levelFor(136_799, DEFAULT_THRESHOLDS));
        assertEquals(20, LevelCurve.levelFor(136_800, DEFAULT_THRESHOLDS));
        assertEquals(20, LevelCurve.levelFor(200_000, DEFAULT_THRESHOLDS));
    }

    @Test
    void nextThresholdReturnsNextOrEmptyAtMax() {
        assertEquals(OptionalInt.of(300), LevelCurve.nextThreshold(0, DEFAULT_THRESHOLDS));
        assertEquals(OptionalInt.of(300), LevelCurve.nextThreshold(299, DEFAULT_THRESHOLDS));
        assertEquals(OptionalInt.of(800), LevelCurve.nextThreshold(300, DEFAULT_THRESHOLDS));
        assertEquals(OptionalInt.of(136_800), LevelCurve.nextThreshold(136_799, DEFAULT_THRESHOLDS));
        assertEquals(OptionalInt.empty(), LevelCurve.nextThreshold(136_800, DEFAULT_THRESHOLDS));
        assertEquals(OptionalInt.empty(), LevelCurve.nextThreshold(200_000, DEFAULT_THRESHOLDS));
    }
}
