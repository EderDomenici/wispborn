package dev.eder.wispborn.form;

import dev.eder.wispborn.WispbornMod;
import net.minecraft.resources.ResourceLocation;

public final class WispTuning {
    public static final ResourceLocation SCALE_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_scale");
    public static final ResourceLocation MAX_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_max_health");
    public static final ResourceLocation VITALITY_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_vitality");
    public static final ResourceLocation GRAVITY_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_gravity");
    public static final ResourceLocation SAFE_FALL_DISTANCE_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_safe_fall_distance");
    public static final ResourceLocation MOVEMENT_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_movement_speed");

    public static final double SCALE_MODIFIER = -0.52;
    public static final double MAX_HEALTH_MODIFIER = -14.0;
    public static final double GRAVITY_MODIFIER = -0.75;
    public static final double SAFE_FALL_DISTANCE_MODIFIER = 20.0;
    public static final double MOVEMENT_SPEED_MODIFIER = 0.2;

    /** Escala só do visual do modelo, multiplicada sobre a escala da hitbox (SCALE). Não altera colisão. */
    public static final float MODEL_VISUAL_SCALE = 2.0F;

    /** Intervalo mínimo em ticks entre sons de cintilação do wisp ao andar. */
    public static final int DRIFT_SOUND_INTERVAL_TICKS = 10;

    /** Cores das partículas de indicador de alvo (RGB) e de mob possuído (ARGB). */
    public static final int TARGET_OK_COLOR = 0x40E0D0;
    public static final int TARGET_FAIL_COLOR = 0xFF4444;
    public static final int POSSESSED_PARTICLE_COLOR = 0xFF40E0D0;

    private WispTuning() {
    }
}
