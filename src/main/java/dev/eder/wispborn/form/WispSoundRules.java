package dev.eder.wispborn.form;

/**
 * Regras puras sobre classificação de sons de blocos sob o wisp e cadência do som de brilho (drift).
 */
public final class WispSoundRules {
    public enum SoundKind {
        STEP,
        FALL,
        OTHER
    }

    private WispSoundRules() {
    }

    public static SoundKind classify(String soundPath) {
        if (soundPath == null || !soundPath.startsWith("block.")) {
            return SoundKind.OTHER;
        }
        if (soundPath.endsWith(".step")) {
            return SoundKind.STEP;
        }
        if (soundPath.endsWith(".fall")) {
            return SoundKind.FALL;
        }
        return SoundKind.OTHER;
    }

    public static boolean shouldPlayDrift(long lastTick, long now, int interval) {
        if (lastTick < 0L || now < lastTick) {
            return true;
        }
        return (now - lastTick) >= interval;
    }
}
