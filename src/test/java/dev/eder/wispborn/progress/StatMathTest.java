package dev.eder.wispborn.progress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatMathTest {

    @Test
    void availablePointsCalculation() {
        assertEquals(0, StatMath.availablePoints(1, 2, 0));
        assertEquals(3, StatMath.availablePoints(3, 2, 1));
    }

    @Test
    void drainPercentCalculation() {
        assertEquals(0.04D, StatMath.drainPercent(0.02D, 4, 0.005D), 1e-9);
    }

    @Test
    void applyEpWithEfficiencyMultiplier() {
        double multiplier = StatMath.epMultiplier(1, 0.1D);
        assertEquals(11, StatMath.applyEp(10, multiplier));
    }

    @Test
    void maxManaCalculation() {
        assertEquals(70, StatMath.maxMana(50, 2, 10));
    }

    @Test
    void vitalityBonusHealthCalculation() {
        assertEquals(6.0D, StatMath.vitalityBonusHealth(3, 1), 1e-9);
    }
}
