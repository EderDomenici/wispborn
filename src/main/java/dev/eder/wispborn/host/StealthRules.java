package dev.eder.wispborn.host;

import dev.eder.wispborn.form.FormType;

public final class StealthRules {
    private StealthRules() {
    }

    public static boolean blocksTargeting(boolean targetIsPlayer, FormType targetForm) {
        return targetIsPlayer && targetForm == FormType.HOST;
    }

    public static boolean shouldClear(boolean targetsPlayer, boolean angryAtPlayer, boolean isHostItself) {
        return (targetsPlayer || angryAtPlayer) && !isHostItself;
    }
}
