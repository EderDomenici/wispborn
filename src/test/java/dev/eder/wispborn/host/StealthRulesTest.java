package dev.eder.wispborn.host;

import dev.eder.wispborn.form.FormType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StealthRulesTest {

    @Test
    void blocksTargetingOnlyForHostPlayer() {
        assertTrue(StealthRules.blocksTargeting(true, FormType.HOST));
        assertFalse(StealthRules.blocksTargeting(true, FormType.WISP));
        assertFalse(StealthRules.blocksTargeting(false, FormType.HOST));
        assertFalse(StealthRules.blocksTargeting(false, FormType.WISP));
    }

    @Test
    void shouldClearWhenTargetingOrAngry() {
        assertTrue(StealthRules.shouldClear(true, false));
        assertTrue(StealthRules.shouldClear(false, true));
        assertTrue(StealthRules.shouldClear(true, true));
        assertFalse(StealthRules.shouldClear(false, false));
    }
}
