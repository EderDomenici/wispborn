package dev.eder.wispborn.traits;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobTraitRollerTest {

    @Test
    void rollLevelStaysWithinRangeAndCoversAllValues() {
        RandomSource random = RandomSource.create(42L);
        MobLevelRange range = new MobLevelRange(4, 8);
        Set<Integer> seen = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            MobTraits traits = MobTraitRoller.roll(range, 0.5D, random);
            assertTrue(traits.level() >= 4 && traits.level() <= 8);
            seen.add(traits.level());
        }

        assertEquals(Set.of(4, 5, 6, 7, 8), seen);
    }

    @Test
    void rollSexRespectsFemaleChanceExtremesAndMidpoint() {
        MobLevelRange range = new MobLevelRange(1, 3);

        RandomSource rZero = RandomSource.create(42L);
        for (int i = 0; i < 200; i++) {
            assertEquals(Sex.MALE, MobTraitRoller.roll(range, 0.0D, rZero).sex());
        }

        RandomSource rOne = RandomSource.create(42L);
        for (int i = 0; i < 200; i++) {
            assertEquals(Sex.FEMALE, MobTraitRoller.roll(range, 1.0D, rOne).sex());
        }

        RandomSource rHalf = RandomSource.create(42L);
        int females = 0;
        for (int i = 0; i < 1000; i++) {
            if (MobTraitRoller.roll(range, 0.5D, rHalf).sex() == Sex.FEMALE) {
                females++;
            }
        }
        assertTrue(females >= 400 && females <= 600, "Females count out of [400, 600]: " + females);
    }
}
