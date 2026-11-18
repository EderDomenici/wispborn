package dev.eder.wispborn.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WispbornClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue NAMEPLATE_RANGE =
            BUILDER.defineInRange("client.nameplateRange", 8, 1, 64);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WispbornClientConfig() {
    }
}
