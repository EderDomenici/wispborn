package dev.eder.wispborn.form;

import net.minecraft.world.Difficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HungerMathTest {

    @Test
    void exhaustionPerSecondScalesWithLevel() {
        assertEquals(0.06F, HungerMath.exhaustionPerSecond(1, 0.05D, 0.01D), 1e-6F);
        assertEquals(0.25F, HungerMath.exhaustionPerSecond(20, 0.05D, 0.01D), 1e-6F);
    }

    @Test
    void vanillaStarvesMatchesFoodDataRules() {
        assertTrue(HungerMath.vanillaStarves(Difficulty.HARD, 1.0F));
        assertFalse(HungerMath.vanillaStarves(Difficulty.NORMAL, 1.0F));
        assertTrue(HungerMath.vanillaStarves(Difficulty.NORMAL, 2.0F));
        assertFalse(HungerMath.vanillaStarves(Difficulty.EASY, 6.0F));
        assertTrue(HungerMath.vanillaStarves(Difficulty.EASY, 11.0F));
    }

    @Test
    void wispStarvesOnlyWhenWispHungryNonPeacefulAndVanillaDoesNotStarve() {
        assertTrue(HungerMath.wispStarves(true, Difficulty.EASY, 0, 6.0F));
        assertFalse(HungerMath.wispStarves(true, Difficulty.HARD, 0, 6.0F));
        assertFalse(HungerMath.wispStarves(true, Difficulty.EASY, 1, 6.0F));
        assertFalse(HungerMath.wispStarves(false, Difficulty.EASY, 0, 6.0F));
        assertFalse(HungerMath.wispStarves(true, Difficulty.PEACEFUL, 0, 6.0F));
    }

    @Test
    void consumeFoodScalesWithHalfMobLevel() {
        assertEquals(2, HungerMath.consumeFood(1, 2));
        assertEquals(6, HungerMath.consumeFood(9, 2));
        assertEquals(12, HungerMath.consumeFood(20, 2));
    }
}
