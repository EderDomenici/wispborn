package dev.eder.wispborn.progress;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TierResolverTest {
    private static final List<Integer> DEFAULT_MIN_LEVELS = List.of(1, 3, 5);

    @Test
    void maxTierForLevelMatchesThresholds() {
        assertEquals(1, TierResolver.maxTierFor(1, DEFAULT_MIN_LEVELS));
        assertEquals(1, TierResolver.maxTierFor(2, DEFAULT_MIN_LEVELS));
        assertEquals(2, TierResolver.maxTierFor(3, DEFAULT_MIN_LEVELS));
        assertEquals(3, TierResolver.maxTierFor(5, DEFAULT_MIN_LEVELS));
        assertEquals(3, TierResolver.maxTierFor(10, DEFAULT_MIN_LEVELS));
    }
}
