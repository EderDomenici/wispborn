package dev.eder.wispborn.form;

/**
 * Regras puras sobre quais sons o wisp não emite.
 */
public final class WispSoundRules {
    private WispSoundRules() {
    }

    /**
     * Sons de passo e de queda vindos do bloco embaixo (ex.: {@code block.grass.step}, {@code block.stone.fall}).
     * O wisp não tem pés: esses sons são silenciados.
     */
    public static boolean isFootstepSound(String soundPath) {
        return soundPath.startsWith("block.") && (soundPath.endsWith(".step") || soundPath.endsWith(".fall"));
    }
}
