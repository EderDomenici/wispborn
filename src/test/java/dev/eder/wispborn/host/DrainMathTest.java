package dev.eder.wispborn.host;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrainMathTest {

    @Test
    void nextHealthDrainsAndClampsAtFloor() {
        assertEquals(9.8F, DrainMath.nextHealth(10.0F, 10.0F, 0.02D, 0.2D), 1e-5F);
        assertEquals(9.6F, DrainMath.nextHealth(10.0F, 10.0F, 0.04D, 0.2D), 1e-5F);
        assertEquals(2.0F, DrainMath.nextHealth(2.1F, 10.0F, 0.02D, 0.2D), 1e-5F);
        assertEquals(2.0F, DrainMath.nextHealth(2.0F, 10.0F, 0.02D, 0.2D), 1e-5F);
        assertEquals(1.5F, DrainMath.nextHealth(1.5F, 10.0F, 0.02D, 0.2D), 1e-5F);
    }

    @Test
    void atFloorDetectsThreshold() {
        assertTrue(DrainMath.atFloor(2.0F, 10.0F, 0.2D));
        assertTrue(DrainMath.atFloor(1.5F, 10.0F, 0.2D));
        assertFalse(DrainMath.atFloor(2.01F, 10.0F, 0.2D));
    }

    @Test
    void reachesFloorInExactlyFortyPulsesAtTwoPercentPerPulse() {
        float health = 10.0F;
        int pulses = 0;
        while (!DrainMath.atFloor(health, 10.0F, 0.2D)) {
            health = DrainMath.nextHealth(health, 10.0F, 0.02D, 0.2D);
            pulses++;
        }
        assertEquals(40, pulses);
        assertEquals(2.0F, health, 1e-5F);
    }

    @Test
    void epPerPulseRoundsCorrectly() {
        assertEquals(6, DrainMath.epPerPulse(2, 3, 1.0D));
        assertEquals(27, DrainMath.epPerPulse(9, 3, 1.0D));
        assertEquals(14, DrainMath.epPerPulse(9, 3, 0.5D));
        assertEquals(0, DrainMath.epPerPulse(9, 3, 0.0D));
    }
}
