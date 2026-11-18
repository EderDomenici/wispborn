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
    void shouldClearWhenTargetingOrAngryExceptForHostItself() {
        assertTrue(StealthRules.shouldClear(true, false, false));
        assertTrue(StealthRules.shouldClear(false, true, false));
        assertTrue(StealthRules.shouldClear(true, true, false));
        assertFalse(StealthRules.shouldClear(false, false, false));
        assertFalse(StealthRules.shouldClear(true, false, true));
        assertFalse(StealthRules.shouldClear(false, true, true));
        assertFalse(StealthRules.shouldClear(true, true, true));
    }
}
