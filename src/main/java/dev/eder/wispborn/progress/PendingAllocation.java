package dev.eder.wispborn.progress;

public final class PendingAllocation {
    private WispStats committed;
    private int availablePoints;
    private int maxPerStat;
    private StatDelta delta = StatDelta.ZERO;

    public PendingAllocation(WispStats committed, int availablePoints, int maxPerStat) {
        this.committed = committed;
        this.availablePoints = Math.max(0, availablePoints);
        this.maxPerStat = maxPerStat;
    }

    public boolean canAdd(Stat stat) {
        return remainingPoints() > 0 && total(stat) < this.maxPerStat;
    }

    public boolean add(Stat stat) {
        if (!canAdd(stat)) {
            return false;
        }
        this.delta = this.delta.with(stat, this.delta.get(stat) + 1);
        return true;
    }

    public boolean canRemove(Stat stat) {
        return this.delta.get(stat) > 0;
    }

    public boolean remove(Stat stat) {
        if (!canRemove(stat)) {
            return false;
        }
        this.delta = this.delta.with(stat, this.delta.get(stat) - 1);
        return true;
    }

    public int committed(Stat stat) {
        return this.committed.get(stat);
    }

    public int pending(Stat stat) {
        return this.delta.get(stat);
    }

    public int total(Stat stat) {
        return this.committed.get(stat) + this.delta.get(stat);
    }

    public int remainingPoints() {
        return Math.max(0, this.availablePoints - this.delta.total());
    }

    public boolean hasPending() {
        return this.delta.total() > 0;
    }

    public void clear() {
        this.delta = StatDelta.ZERO;
    }

    public StatDelta toDelta() {
        return this.delta;
    }

    public void rebase(WispStats committed, int availablePoints, int maxPerStat) {
        this.committed = committed;
        this.availablePoints = Math.max(0, availablePoints);
        this.maxPerStat = maxPerStat;
        if (this.delta.total() > 0) {
            BatchAllocateResult check = AllocationRules.check(
                    this.availablePoints,
                    this.committed,
                    this.delta,
                    this.maxPerStat
            );
            if (check != BatchAllocateResult.OK) {
                this.delta = StatDelta.ZERO;
            }
        }
    }
}
