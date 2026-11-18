package dev.eder.wispborn.host;

import dev.eder.wispborn.traits.MobTraits;

import java.util.Optional;

public final class InfestRules {
    private InfestRules() {
    }

    public static InfestResult check(
            boolean isMob,
            boolean alive,
            boolean hasPassenger,
            Optional<MobTraits> traits,
            int wispLevel,
            int levelGateMargin,
            float mana,
            int costPerLevel
    ) {
        if (!isMob || !alive) {
            return InfestResult.NOT_A_MOB;
        }
        if (hasPassenger) {
            return InfestResult.HAS_PASSENGER;
        }
        if (traits.isEmpty()) {
            return InfestResult.IMMUNE;
        }
        int mobLevel = traits.get().level();
        if (mobLevel > wispLevel + levelGateMargin) {
            return InfestResult.TOO_STRONG;
        }
        float requiredMana = (float) costPerLevel * mobLevel;
        if (mana < requiredMana) {
            return InfestResult.NO_MANA;
        }
        return InfestResult.OK;
    }
}
