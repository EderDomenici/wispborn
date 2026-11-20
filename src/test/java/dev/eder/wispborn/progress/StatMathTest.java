package dev.eder.wispborn.progress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(50, StatMath.maxMana(50, 1, 5, 0, 10));
        assertEquals(70, StatMath.maxMana(50, 1, 5, 2, 10));
        assertEquals(145, StatMath.maxMana(50, 20, 5, 0, 10));
        assertEquals(245, StatMath.maxMana(50, 20, 5, 10, 10));
    }

    @Test
    void maxManaAlwaysPaysForMobOfSameLevel() {
        int costPerLevel = 5;
        for (int level = 1; level <= 20; level++) {
            assertTrue(StatMath.maxMana(50, level, 5, 0, 10) >= costPerLevel * level, "Nv " + level);
        }
    }

    @Test
    void vitalityBonusHealthCalculation() {
        assertEquals(6.0D, StatMath.vitalityBonusHealth(3, 1), 1e-9);
    }
}
