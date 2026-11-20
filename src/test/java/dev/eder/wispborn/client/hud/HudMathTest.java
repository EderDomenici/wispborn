package dev.eder.wispborn.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HudMathTest {

    @Test
    void progressWidthClampsAndCropsCorrectly() {
        assertEquals(109, HudMath.progressWidth(42.0F, 70.0F, 182));
        assertEquals(0, HudMath.progressWidth(0.0F, 70.0F, 182));
        assertEquals(182, HudMath.progressWidth(80.0F, 70.0F, 182));
        assertEquals(0, HudMath.progressWidth(10.0F, 0.0F, 182));
        assertEquals(101, HudMath.progressWidth(62.0F, 100.0F, 164));
    }

    @Test
    void iconStatesComputesFullHalfAndEmptySlots() {
        assertArrayEquals(new int[]{2, 2, 1}, HudMath.iconStates(5.0F, 3));
        assertArrayEquals(new int[]{0, 0, 0}, HudMath.iconStates(0.0F, 3));
        assertArrayEquals(new int[]{2, 2, 2}, HudMath.iconStates(6.0F, 3));
    }

    @Test
    void floorMarkerXCalculatesPixelOffset() {
        assertEquals(20, HudMath.floorMarkerX(0.2D, 100));
        assertEquals(32, HudMath.floorMarkerX(0.2D, 164));
    }
}
