package dev.eder.wispborn.progress;

import java.util.List;
import java.util.OptionalInt;

public final class LevelCurve {
    private LevelCurve() {
    }

    public static int levelFor(int totalEp, List<Integer> thresholds) {
        int level = 1;
        for (int threshold : thresholds) {
            if (totalEp >= threshold) {
                level++;
            } else {
                break;
            }
        }
        return level;
    }

    public static OptionalInt nextThreshold(int totalEp, List<Integer> thresholds) {
        for (int threshold : thresholds) {
            if (totalEp < threshold) {
                return OptionalInt.of(threshold);
            }
        }
        return OptionalInt.empty();
    }
}
