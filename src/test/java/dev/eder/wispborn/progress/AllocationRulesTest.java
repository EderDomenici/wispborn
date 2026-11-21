package dev.eder.wispborn.progress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllocationRulesTest {
    private static final WispStats CURRENT = new WispStats(2, 3, 4, 1);
    private static final int MAX = 10;

    @Test
    void checkValidatesAllBatchAllocationCases() {
        assertEquals(
                BatchAllocateResult.OK,
                AllocationRules.check(2, CURRENT, new StatDelta(1, 0, 0, 1), MAX)
        );
        assertEquals(
                BatchAllocateResult.EMPTY,
                AllocationRules.check(2, CURRENT, StatDelta.ZERO, MAX)
        );
        assertEquals(
                BatchAllocateResult.NEGATIVE,
                AllocationRules.check(2, CURRENT, new StatDelta(-1, 0, 0, 1), MAX)
        );
        assertEquals(
                BatchAllocateResult.NOT_ENOUGH_POINTS,
                AllocationRules.check(1, CURRENT, new StatDelta(1, 0, 0, 1), MAX)
        );
        assertEquals(
                BatchAllocateResult.OVER_MAX,
                AllocationRules.check(9, CURRENT, new StatDelta(0, 0, 7, 0), MAX)
        );
        assertEquals(
                BatchAllocateResult.OK,
                AllocationRules.check(6, CURRENT, new StatDelta(0, 0, 6, 0), MAX)
        );
    }

    @Test
    void rejectsHugeDeltaThatWouldOverflow() {
        // Cliente modificado manda VAR_INT enormes: a soma dá a volta e ficaria <= pontos disponíveis.
        StatDelta delta = new StatDelta(Integer.MAX_VALUE, Integer.MAX_VALUE, 2, 0);
        assertEquals(BatchAllocateResult.OVER_MAX, AllocationRules.check(4, WispStats.ZERO, delta, MAX));
    }
}
