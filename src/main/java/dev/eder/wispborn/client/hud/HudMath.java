package dev.eder.wispborn.client.hud;

public final class HudMath {
    private HudMath() {
    }

    public static int progressWidth(float value, float max, int fullWidth) {
        if (max <= 0.0F || fullWidth <= 0 || value <= 0.0F) {
            return 0;
        }
        if (value >= max) {
            return fullWidth;
        }
        double ratio = (double) value / (double) max;
        return Math.clamp((int) Math.floor(ratio * fullWidth), 0, fullWidth);
    }

    public static int[] iconStates(float value, int icons) {
        if (icons <= 0) {
            return new int[0];
        }
        int[] states = new int[icons];
        int points = Math.max(0, (int) Math.ceil(value));
        for (int i = 0; i < icons; i++) {
            states[i] = Math.clamp(points - (i * 2), 0, 2);
        }
        return states;
    }

    /** Y do slot do topo: {@code defaultTop}, ou 2 px abaixo da última barra de boss quando houver. */
    public static int topSlotY(int defaultTop, int bossBottom) {
        return bossBottom > 0 ? Math.max(defaultTop, bossBottom + 2) : defaultTop;
    }

    /** Largura do cartão do alvo: maior linha + margens, entre {@code min} e {@code max}. */
    public static int cardWidth(int widestLine, int padding, int min, int max) {
        return Math.clamp(widestLine + padding * 2L, min, max);
    }

    public static int floorMarkerX(double floorPercent, int barWidth) {
        if (barWidth <= 0) {
            return 0;
        }
        double clamped = Math.clamp(floorPercent, 0.0D, 1.0D);
        return Math.clamp((int) Math.floor(clamped * barWidth + 1.0e-9D), 0, barWidth);
    }
}
