package dev.eder.wispborn.traits;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreedingRulesTest {

    @Test
    void canMateRequiresOppositeSexUnlessAbsent() {
        assertTrue(BreedingRules.canMate(Optional.of(Sex.MALE), Optional.of(Sex.FEMALE)));
        assertTrue(BreedingRules.canMate(Optional.of(Sex.FEMALE), Optional.of(Sex.MALE)));
        assertFalse(BreedingRules.canMate(Optional.of(Sex.MALE), Optional.of(Sex.MALE)));
        assertFalse(BreedingRules.canMate(Optional.of(Sex.FEMALE), Optional.of(Sex.FEMALE)));
        assertTrue(BreedingRules.canMate(Optional.empty(), Optional.of(Sex.MALE)));
        assertTrue(BreedingRules.canMate(Optional.of(Sex.FEMALE), Optional.empty()));
        assertTrue(BreedingRules.canMate(Optional.empty(), Optional.empty()));
    }
}
