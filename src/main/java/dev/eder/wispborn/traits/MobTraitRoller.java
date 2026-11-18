package dev.eder.wispborn.traits;

import net.minecraft.util.RandomSource;

public final class MobTraitRoller {
    private MobTraitRoller() {
    }

    public static MobTraits roll(MobLevelRange range, double femaleChance, RandomSource random) {
        int span = range.max() - range.min() + 1;
        int level = range.min() + random.nextInt(span);
        Sex sex = random.nextDouble() < femaleChance ? Sex.FEMALE : Sex.MALE;
        return new MobTraits(level, sex);
    }
}
