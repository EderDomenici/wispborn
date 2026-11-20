package dev.eder.wispborn.host;

public final class DrainMath {
    private static final float FLOOR_EPSILON = 1.0e-4F;

    private DrainMath() {
    }

    public static float nextHealth(float health, float max, double percent, double floorPercent) {
        if (atFloor(health, max, floorPercent)) {
            return health;
        }
        float floor = (float) (max * floorPercent);
        float candidate = (float) (health - (max * percent));
        if (candidate <= floor + FLOOR_EPSILON) {
            return floor;
        }
        return candidate;
    }

    public static boolean atFloor(float health, float max, double floorPercent) {
        float floor = (float) (max * floorPercent);
        return health <= floor + FLOOR_EPSILON;
    }

    public static int epPerPulse(int mobLevel, int perPulsePerLevel, double factor) {
        if (mobLevel <= 0 || perPulsePerLevel <= 0 || factor <= 0.0D) {
            return 0;
        }
        return (int) Math.round(perPulsePerLevel * mobLevel * factor);
    }
}
