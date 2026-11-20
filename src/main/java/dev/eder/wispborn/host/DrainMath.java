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

    /**
     * Tom do som do pulso: 0,8 com o hospedeiro cheio, 1,3 quando chega ao piso (o som "aperta"
     * conforme a vida acaba).
     */
    public static float pulsePitch(float health, float max, double floorPercent) {
        if (max <= 0.0F || floorPercent >= 1.0D) {
            return 1.3F;
        }
        double remaining = ((health / max) - floorPercent) / (1.0D - floorPercent);
        double clamped = Math.max(0.0D, Math.min(1.0D, remaining));
        return (float) (0.8D + 0.5D * (1.0D - clamped));
    }

    /** Raio do anel no passo {@code step} de {@code steps}, interpolando de {@code start} a {@code end}. */
    public static float ringRadius(int step, int steps, float start, float end) {
        if (steps <= 0) {
            return end;
        }
        int clamped = Math.max(0, Math.min(steps, step));
        return start + (end - start) * clamped / steps;
    }

    /** Quantidade de partículas do anel: cerca de 3 por bloco de circunferência, entre 10 e 40. */
    public static int ringPoints(float radius) {
        int byLength = Math.round((float) (Math.PI * 2.0D * radius * 3.0D));
        return Math.max(10, Math.min(40, byLength));
    }

    public static int epPerPulse(int mobLevel, int perPulsePerLevel, double factor) {
        if (mobLevel <= 0 || perPulsePerLevel <= 0 || factor <= 0.0D) {
            return 0;
        }
        return (int) Math.round(perPulsePerLevel * mobLevel * factor);
    }
}
