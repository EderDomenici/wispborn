package dev.eder.wispborn.host;

import dev.eder.wispborn.form.FormType;

public final class StealthRules {
    private StealthRules() {
    }

    public static boolean blocksTargeting(boolean targetIsPlayer, FormType targetForm) {
        return targetIsPlayer && targetForm == FormType.HOST;
    }

    /**
     * Inclui o próprio hospedeiro: a limpeza só apaga o que aponta para o jogador, e um hospedeiro com
     * brain (hoglin, piglin) não passa pelo LivingChangeTargetEvent enquanto mantém a memória de alvo.
     */
    public static boolean shouldClear(boolean targetsPlayer, boolean angryAtPlayer) {
        return targetsPlayer || angryAtPlayer;
    }
}
