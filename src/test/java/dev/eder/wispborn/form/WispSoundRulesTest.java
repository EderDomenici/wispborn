package dev.eder.wispborn.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WispSoundRulesTest {

    @Test
    void classifyDistinguishesStepFallAndOtherSounds() {
        assertEquals(WispSoundRules.SoundKind.STEP, WispSoundRules.classify("block.grass.step"));
        assertEquals(WispSoundRules.SoundKind.STEP, WispSoundRules.classify("block.stone.step"));
        assertEquals(WispSoundRules.SoundKind.FALL, WispSoundRules.classify("block.wool.fall"));

        assertEquals(WispSoundRules.SoundKind.OTHER, WispSoundRules.classify("entity.player.hurt"));
        assertEquals(WispSoundRules.SoundKind.OTHER, WispSoundRules.classify("block.stone.break"));
        assertEquals(WispSoundRules.SoundKind.OTHER, WispSoundRules.classify("block.chest.open"));
        assertEquals(WispSoundRules.SoundKind.OTHER, WispSoundRules.classify("entity.allay.item_taken"));
    }

    @Test
    void shouldPlayDriftRespectsInterval() {
        assertTrue(WispSoundRules.shouldPlayDrift(-1L, 0L, 10));
        assertFalse(WispSoundRules.shouldPlayDrift(100L, 105L, 10));
        assertFalse(WispSoundRules.shouldPlayDrift(100L, 109L, 10));
        assertTrue(WispSoundRules.shouldPlayDrift(100L, 110L, 10));
        assertTrue(WispSoundRules.shouldPlayDrift(100L, 125L, 10));
    }
}
