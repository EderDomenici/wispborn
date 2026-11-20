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

    /**
     * Mana máxima = base + perLevel × (nível − 1) + perPoint × pontos em Reserva.
     * Com os padrões (50, 5, 10) e custo de infestação 5 × nível do mob, mana cheia sempre
     * paga um mob do mesmo nível do wisp; a Reserva dá folga para encadear possessões.
     */
    public static int maxMana(int base, int level, int perLevel, int reservePts, int perPoint) {
        return base + perLevel * Math.max(0, level - 1) + perPoint * reservePts;
    }

    public static double vitalityBonusHealth(int vitalityPts, int heartsPerPoint) {
        return 2.0D * heartsPerPoint * vitalityPts;
    }
}
