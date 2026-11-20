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

    public static int levelStartEp(int level, List<Integer> thresholds) {
        if (level <= 1 || thresholds.isEmpty()) {
            return 0;
        }
        int index = Math.min(level - 2, thresholds.size() - 1);
        return thresholds.get(index);
    }
}
