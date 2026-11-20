package dev.eder.wispborn.form;

import net.minecraft.world.Difficulty;

public final class HungerMath {
    private HungerMath() {
    }

    public static float exhaustionPerSecond(int level, double base, double perLevel) {
        return (float) (base + perLevel * level);
    }

    public static boolean vanillaStarves(Difficulty difficulty, float health) {
        return health > 10.0F || difficulty == Difficulty.HARD || (difficulty == Difficulty.NORMAL && health > 1.0F);
    }

    public static boolean wispStarves(boolean isWisp, Difficulty difficulty, int foodLevel, float health) {
        return isWisp && difficulty != Difficulty.PEACEFUL && foodLevel == 0 && !vanillaStarves(difficulty, health);
    }

    public static int consumeFood(int mobLevel, int base) {
        return base + (mobLevel / 2);
    }
}
