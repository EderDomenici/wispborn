package dev.eder.wispborn.host;

import dev.eder.wispborn.traits.MobTraits;
import dev.eder.wispborn.traits.Sex;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfestRulesTest {

    @Test
    void enforcesFullPrecedenceOrder() {
        Optional<MobTraits> lv5 = Optional.of(new MobTraits(5, Sex.FEMALE));

        assertEquals(
                InfestResult.NOT_A_MOB,
                InfestRules.check(false, true, true, Optional.empty(), 1, 0, 0.0F, 5)
        );
        assertEquals(
                InfestResult.NOT_A_MOB,
                InfestRules.check(true, false, true, Optional.empty(), 1, 0, 0.0F, 5)
        );
        assertEquals(
                InfestResult.HAS_PASSENGER,
                InfestRules.check(true, true, true, Optional.empty(), 1, 0, 0.0F, 5)
        );
        assertEquals(
                InfestResult.IMMUNE,
                InfestRules.check(true, true, false, Optional.empty(), 10, 0, 0.0F, 5)
        );
        assertEquals(
                InfestResult.TOO_STRONG,
                InfestRules.check(true, true, false, lv5, 4, 0, 0.0F, 5)
        );
        assertEquals(
                InfestResult.NO_MANA,
                InfestRules.check(true, true, false, lv5, 5, 0, 24.99F, 5)
        );
    }

    @Test
    void respectsLevelGateMarginAndExactMana() {
        Optional<MobTraits> lv5 = Optional.of(new MobTraits(5, Sex.MALE));

        assertEquals(
                InfestResult.OK,
                InfestRules.check(true, true, false, lv5, 5, 0, 25.0F, 5)
        );
        assertEquals(
                InfestResult.TOO_STRONG,
                InfestRules.check(true, true, false, Optional.of(new MobTraits(6, Sex.MALE)), 5, 0, 100.0F, 5)
        );
        assertEquals(
                InfestResult.OK,
                InfestRules.check(true, true, false, Optional.of(new MobTraits(6, Sex.MALE)), 5, 1, 30.0F, 5)
        );
        assertEquals(
                InfestResult.NO_MANA,
                InfestRules.check(true, true, false, lv5, 5, 0, 25.0F - 0.01F, 5)
        );
    }
}
