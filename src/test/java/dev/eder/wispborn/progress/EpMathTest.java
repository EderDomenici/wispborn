package dev.eder.wispborn.progress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpMathTest {

    @Test
    void levelFactorAppliesGraceAndFalloff() {
        assertEquals(1.0D, EpMath.levelFactor(7, 7, 2, 0.25D), 1e-9D);
        assertEquals(1.0D, EpMath.levelFactor(7, 5, 2, 0.25D), 1e-9D);
        assertEquals(0.75D, EpMath.levelFactor(7, 4, 2, 0.25D), 1e-9D);
        assertEquals(0.5D, EpMath.levelFactor(7, 3, 2, 0.25D), 1e-9D);
        assertEquals(0.25D, EpMath.levelFactor(7, 2, 2, 0.25D), 1e-9D);
        assertEquals(0.0D, EpMath.levelFactor(7, 1, 2, 0.25D), 1e-9D);
        assertEquals(1.0D, EpMath.levelFactor(5, 9, 2, 0.25D), 1e-9D);
    }
}
