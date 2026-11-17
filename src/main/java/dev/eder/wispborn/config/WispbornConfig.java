package dev.eder.wispborn.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class WispbornConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> LEVEL_THRESHOLDS =
            BUILDER.defineListAllowEmpty(
                    "levels.thresholds",
                    List.of(300, 800, 1600, 2800, 4500, 6800, 9800, 13600, 18300),
                    () -> 300,
                    o -> o instanceof Integer i && i > 0
            );

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> TIERS_MIN_LEVEL =
            BUILDER.defineListAllowEmpty(
                    "tiers.minLevel",
                    List.of(1, 3, 5),
                    () -> 1,
                    o -> o instanceof Integer i && i >= 1
            );

    public static final ModConfigSpec.IntValue POINTS_PER_LEVEL =
            BUILDER.defineInRange("stats.pointsPerLevel", 2, 0, 100);

    public static final ModConfigSpec.IntValue MAX_PER_STAT =
            BUILDER.defineInRange("stats.maxPerStat", 10, 1, 100);

    public static final ModConfigSpec.DoubleValue ABSORPTION_PER_POINT =
            BUILDER.defineInRange("stats.absorptionPerPoint", 0.005D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue EFFICIENCY_PER_POINT =
            BUILDER.defineInRange("stats.efficiencyPerPoint", 0.10D, 0.0D, 10.0D);

    public static final ModConfigSpec.IntValue RESERVE_PER_POINT =
            BUILDER.defineInRange("stats.reservePerPoint", 10, 0, 10000);

    public static final ModConfigSpec.IntValue VITALITY_HEARTS_PER_POINT =
            BUILDER.defineInRange("stats.vitalityHeartsPerPoint", 1, 0, 100);

    public static final ModConfigSpec.IntValue MANA_BASE =
            BUILDER.defineInRange("mana.base", 50, 0, 100000);

    public static final ModConfigSpec.DoubleValue MANA_REGEN_PER_SECOND =
            BUILDER.defineInRange("mana.regenPerSecond", 1.0D, 0.0D, 10000.0D);

    public static final ModConfigSpec.IntValue MANA_INFEST_COST_PER_TIER =
            BUILDER.defineInRange("mana.infestCostPerTier", 15, 0, 100000);

    public static final ModConfigSpec.IntValue DRAIN_INTERVAL_TICKS =
            BUILDER.defineInRange("drain.intervalTicks", 100, 1, 72000);

    public static final ModConfigSpec.DoubleValue DRAIN_BASE_PERCENT_PER_PULSE =
            BUILDER.defineInRange("drain.basePercentPerPulse", 0.02D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue DRAIN_FLOOR_PERCENT =
            BUILDER.defineInRange("drain.floorPercent", 0.20D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue EP_PER_PULSE_PER_TIER =
            BUILDER.defineInRange("ep.perPulsePerTier", 10, 0, 100000);

    public static final ModConfigSpec.IntValue EP_CONSUME_BONUS_PER_TIER =
            BUILDER.defineInRange("ep.consumeBonusPerTier", 100, 0, 1000000);

    public static final ModConfigSpec.IntValue POSSESS_CAST_TICKS =
            BUILDER.defineInRange("possess.castTicks", 20, 1, 1200);

    public static final ModConfigSpec.DoubleValue POSSESS_RANGE =
            BUILDER.defineInRange("possess.range", 4.0D, 1.0D, 32.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WispbornConfig() {
    }

    public static List<Integer> levelThresholds() {
        // Ordenada e sem repetição: LevelCurve assume limites crescentes.
        return LEVEL_THRESHOLDS.get().stream().map(Integer::intValue).sorted().distinct().toList();
    }

    public static List<Integer> tiersMinLevel() {
        return TIERS_MIN_LEVEL.get().stream().map(Integer::intValue).toList();
    }
}
