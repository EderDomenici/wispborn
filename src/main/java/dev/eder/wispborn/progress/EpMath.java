package dev.eder.wispborn.progress;

public final class EpMath {
    private EpMath() {
    }

    public static double levelFactor(int wispLevel, int mobLevel, int grace, double falloffPerLevel) {
        int diff = wispLevel - mobLevel;
        if (diff <= grace) {
            return 1.0D;
        }
        return Math.max(0.0D, 1.0D - falloffPerLevel * (diff - grace));
    }
}
