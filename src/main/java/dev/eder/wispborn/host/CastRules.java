package dev.eder.wispborn.host;

import java.util.Optional;

public final class CastRules {
    private CastRules() {
    }

    public static Optional<CancelReason> cancelReason(
            boolean isWisp,
            boolean targetAlive,
            double distSqr,
            double rangeSqr,
            boolean hasLineOfSight,
            boolean tookDamage
    ) {
        if (!isWisp) {
            return Optional.of(CancelReason.NOT_WISP);
        }
        if (!targetAlive) {
            return Optional.of(CancelReason.TARGET_GONE);
        }
        if (distSqr > rangeSqr) {
            return Optional.of(CancelReason.OUT_OF_RANGE);
        }
        if (!hasLineOfSight) {
            return Optional.of(CancelReason.NO_LINE_OF_SIGHT);
        }
        if (tookDamage) {
            return Optional.of(CancelReason.TOOK_DAMAGE);
        }
        return Optional.empty();
    }
}
