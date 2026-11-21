package dev.eder.wispborn.progress;

public final class AllocationRules {
    private AllocationRules() {
    }

    public static BatchAllocateResult check(
            int availablePoints,
            WispStats current,
            StatDelta delta,
            int maxPerStat
    ) {
        for (Stat stat : Stat.values()) {
            if (delta.get(stat) < 0) {
                return BatchAllocateResult.NEGATIVE;
            }
        }
        // Antes de somar: com cada parcela <= maxPerStat, nem total() nem current + delta estouram int.
        for (Stat stat : Stat.values()) {
            if (delta.get(stat) > maxPerStat) {
                return BatchAllocateResult.OVER_MAX;
            }
        }
        int total = delta.total();
        if (total == 0) {
            return BatchAllocateResult.EMPTY;
        }
        if (total > availablePoints) {
            return BatchAllocateResult.NOT_ENOUGH_POINTS;
        }
        for (Stat stat : Stat.values()) {
            if (current.get(stat) + delta.get(stat) > maxPerStat) {
                return BatchAllocateResult.OVER_MAX;
            }
        }
        return BatchAllocateResult.OK;
    }
}
