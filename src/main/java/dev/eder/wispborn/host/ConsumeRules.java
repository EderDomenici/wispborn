package dev.eder.wispborn.host;

public final class ConsumeRules {
    private ConsumeRules() {
    }

    public static ConsumeResult check(boolean isHost, boolean hostAlive, float health, float max, double floorPercent) {
        if (!isHost || !hostAlive) {
            return ConsumeResult.NOT_HOST;
        }
        if (!DrainMath.atFloor(health, max, floorPercent)) {
            return ConsumeResult.NOT_READY;
        }
        return ConsumeResult.OK;
    }

    public static int consumeEp(int mobLevel, int consumePerLevel, double factor) {
        if (mobLevel <= 0 || consumePerLevel <= 0 || factor <= 0.0D) {
            return 0;
        }
        return (int) Math.round(consumePerLevel * mobLevel * factor);
    }
}
