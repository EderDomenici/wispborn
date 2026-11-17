package dev.eder.wispborn.host;

import java.util.OptionalInt;

public final class InfestRules {
    private InfestRules() {
    }

    public static InfestResult check(
            boolean isMob,
            boolean alive,
            boolean hasPassenger,
            OptionalInt tier,
            int maxTier,
            float mana,
            int costPerTier
    ) {
        if (!isMob || !alive) {
            return InfestResult.NOT_A_MOB;
        }
        if (hasPassenger) {
            return InfestResult.HAS_PASSENGER;
        }
        if (tier.isEmpty()) {
            return InfestResult.IMMUNE;
        }
        int targetTier = tier.getAsInt();
        if (targetTier > maxTier) {
            return InfestResult.TOO_STRONG;
        }
        float requiredMana = (float) costPerTier * targetTier;
        if (mana < requiredMana) {
            return InfestResult.NO_MANA;
        }
        return InfestResult.OK;
    }
}
