package dev.eder.wispborn.progress;

import dev.eder.wispborn.registry.ModTags;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.OptionalInt;

public final class TierResolver {
    private TierResolver() {
    }

    public static int maxTierFor(int level, List<Integer> minLevels) {
        int maxTier = 0;
        for (int i = 0; i < minLevels.size(); i++) {
            if (level >= minLevels.get(i)) {
                maxTier = i + 1;
            }
        }
        return maxTier;
    }

    public static OptionalInt tierOf(EntityType<?> type) {
        if (type.is(ModTags.TIER_3)) {
            return OptionalInt.of(3);
        }
        if (type.is(ModTags.TIER_2)) {
            return OptionalInt.of(2);
        }
        if (type.is(ModTags.TIER_1)) {
            return OptionalInt.of(1);
        }
        return OptionalInt.empty();
    }
}
