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

    @Test
    void pulsePitchRisesAsHostWeakens() {
        assertEquals(0.8F, DrainMath.pulsePitch(10.0F, 10.0F, 0.2D), 1e-5F);
        assertEquals(1.05F, DrainMath.pulsePitch(6.0F, 10.0F, 0.2D), 1e-5F);
        assertEquals(1.3F, DrainMath.pulsePitch(2.0F, 10.0F, 0.2D), 1e-5F);
        assertEquals(1.3F, DrainMath.pulsePitch(1.0F, 10.0F, 0.2D), 1e-5F);
        assertEquals(1.3F, DrainMath.pulsePitch(5.0F, 0.0F, 0.2D), 1e-5F);
        assertEquals(1.3F, DrainMath.pulsePitch(5.0F, 10.0F, 1.0D), 1e-5F);
    }

    @Test
    void ringRadiusInterpolatesAndClamps() {
        assertEquals(0.6F, DrainMath.ringRadius(0, 5, 0.6F, 2.0F), 1e-5F);
        assertEquals(1.3F, DrainMath.ringRadius(5, 10, 0.6F, 2.0F), 1e-5F);
        assertEquals(2.0F, DrainMath.ringRadius(5, 5, 0.6F, 2.0F), 1e-5F);
        assertEquals(2.0F, DrainMath.ringRadius(9, 5, 0.6F, 2.0F), 1e-5F);
        assertEquals(2.0F, DrainMath.ringRadius(1, 0, 0.6F, 2.0F), 1e-5F);
    }

    @Test
    void ringPointsScaleWithRadiusWithinLimits() {
        assertEquals(10, DrainMath.ringPoints(0.1F));
        assertEquals(19, DrainMath.ringPoints(1.0F));
        assertEquals(40, DrainMath.ringPoints(5.0F));
    }
}
