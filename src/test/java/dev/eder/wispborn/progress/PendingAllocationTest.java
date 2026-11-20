package dev.eder.wispborn.progress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingAllocationTest {
    private static final WispStats COMMITTED = new WispStats(2, 3, 4, 1);

    @Test
    void addRespectsAvailablePointsAndMax() {
        PendingAllocation pending = new PendingAllocation(COMMITTED, 2, 10);
        assertTrue(pending.add(Stat.ABSORPTION));
        assertTrue(pending.add(Stat.ABSORPTION));
        assertFalse(pending.add(Stat.ABSORPTION));
        assertEquals(0, pending.remainingPoints());
        assertEquals(2, pending.pending(Stat.ABSORPTION));
        assertEquals(4, pending.total(Stat.ABSORPTION));
    }

    @Test
    void removeOnlyRemovesPendingPoints() {
        PendingAllocation pending = new PendingAllocation(COMMITTED, 2, 10);
        assertFalse(pending.remove(Stat.ABSORPTION));
        assertEquals(2, pending.total(Stat.ABSORPTION));

        assertTrue(pending.add(Stat.ABSORPTION));
        assertEquals(3, pending.total(Stat.ABSORPTION));
        assertTrue(pending.remove(Stat.ABSORPTION));
        assertEquals(2, pending.total(Stat.ABSORPTION));
        assertFalse(pending.remove(Stat.ABSORPTION));
    }

    @Test
    void canAddReturnsFalseWhenStatAtMax() {
        WispStats maxedReserve = new WispStats(2, 3, 10, 1);
        PendingAllocation pending = new PendingAllocation(maxedReserve, 2, 10);
        assertFalse(pending.canAdd(Stat.RESERVE));
        assertFalse(pending.add(Stat.RESERVE));
    }

    @Test
    void toDeltaAndClearWorkAsExpected() {
        PendingAllocation pending = new PendingAllocation(COMMITTED, 2, 10);
        assertTrue(pending.add(Stat.VITALITY));
        assertEquals(new StatDelta(0, 0, 0, 1), pending.toDelta());
        assertTrue(pending.hasPending());

        pending.clear();
        assertFalse(pending.hasPending());
        assertEquals(StatDelta.ZERO, pending.toDelta());
    }

    @Test
    void rebaseKeepsFittingPendingAndClearsInvalidPending() {
        PendingAllocation pending = new PendingAllocation(COMMITTED, 2, 10);
        assertTrue(pending.add(Stat.ABSORPTION));
        assertTrue(pending.add(Stat.EFFICIENCY));

        pending.rebase(COMMITTED, 4, 10);
        assertTrue(pending.hasPending());
        assertEquals(2, pending.remainingPoints());
        assertEquals(new StatDelta(1, 1, 0, 0), pending.toDelta());

        pending.rebase(COMMITTED, 1, 10);
        assertFalse(pending.hasPending());
        assertEquals(1, pending.remainingPoints());
        assertEquals(StatDelta.ZERO, pending.toDelta());
    }
}
