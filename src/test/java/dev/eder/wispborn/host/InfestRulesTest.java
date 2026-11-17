package dev.eder.wispborn.host;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfestRulesTest {

    @Test
    void rejectsNonMobOrDeadTargetFirst() {
        assertEquals(
                InfestResult.NOT_A_MOB,
                InfestRules.check(false, true, true, OptionalInt.empty(), 1, 0.0F, 15)
        );
        assertEquals(
                InfestResult.NOT_A_MOB,
                InfestRules.check(true, false, true, OptionalInt.empty(), 1, 0.0F, 15)
        );
    }

    @Test
    void rejectsTargetWithPassengerBeforeTierOrMana() {
        assertEquals(
                InfestResult.HAS_PASSENGER,
                InfestRules.check(true, true, true, OptionalInt.empty(), 1, 0.0F, 15)
        );
    }

    @Test
    void rejectsImmuneTargetWithoutTierTagBeforeMana() {
        assertEquals(
                InfestResult.IMMUNE,
                InfestRules.check(true, true, false, OptionalInt.empty(), 3, 0.0F, 15)
        );
    }

    @Test
    void checksTierBeforeMana() {
        assertEquals(
                InfestResult.TOO_STRONG,
                InfestRules.check(true, true, false, OptionalInt.of(2), 1, 0.0F, 15)
        );
    }

    @Test
    void rejectsWhenManaBelowCostTimesTier() {
        assertEquals(
                InfestResult.NO_MANA,
                InfestRules.check(true, true, false, OptionalInt.of(1), 1, 14.9F, 15)
        );
        assertEquals(
                InfestResult.NO_MANA,
                InfestRules.check(true, true, false, OptionalInt.of(2), 2, 29.0F, 15)
        );
    }

    @Test
    void succeedsWhenAllConditionsMet() {
        assertEquals(
                InfestResult.OK,
                InfestRules.check(true, true, false, OptionalInt.of(1), 1, 15.0F, 15)
        );
        assertEquals(
                InfestResult.OK,
                InfestRules.check(true, true, false, OptionalInt.of(2), 3, 30.0F, 15)
        );
    }
}
