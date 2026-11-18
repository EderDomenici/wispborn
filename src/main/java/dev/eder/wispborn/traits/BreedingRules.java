package dev.eder.wispborn.traits;

import java.util.Optional;

public final class BreedingRules {
    private BreedingRules() {
    }

    public static boolean canMate(Optional<Sex> a, Optional<Sex> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return true;
        }
        return a.get() != b.get();
    }
}
