package dev.eder.wispborn.progress;

public final class StatMath {
    private StatMath() {
    }

    public static int availablePoints(int level, int pointsPerLevel, int spent) {
        return pointsPerLevel * (level - 1) - spent;
    }

    public static double drainPercent(double basePercent, int absorptionPts, double perPoint) {
        return basePercent + perPoint * absorptionPts;
    }

    public static double epMultiplier(int efficiencyPts, double perPoint) {
        return 1.0D + perPoint * efficiencyPts;
    }

    public static int applyEp(int baseEp, double multiplier) {
        return (int) Math.round(baseEp * multiplier);
    }

    public static int maxMana(int base, int reservePts, int perPoint) {
        return base + perPoint * reservePts;
    }

    public static double vitalityBonusHealth(int vitalityPts, int heartsPerPoint) {
        return 2.0D * heartsPerPoint * vitalityPts;
    }
}
