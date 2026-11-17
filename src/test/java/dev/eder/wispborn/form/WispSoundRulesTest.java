package dev.eder.wispborn.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WispSoundRulesTest {

    @Test
    void blockStepAndFallSoundsAreFootsteps() {
        assertTrue(WispSoundRules.isFootstepSound("block.grass.step"));
        assertTrue(WispSoundRules.isFootstepSound("block.stone.step"));
        assertTrue(WispSoundRules.isFootstepSound("block.wool.fall"));
    }

    @Test
    void otherSoundsAreNotFootsteps() {
        assertFalse(WispSoundRules.isFootstepSound("entity.player.hurt"));
        assertFalse(WispSoundRules.isFootstepSound("block.stone.break"));
        assertFalse(WispSoundRules.isFootstepSound("block.chest.open"));
        assertFalse(WispSoundRules.isFootstepSound("entity.allay.item_taken"));
    }
}
