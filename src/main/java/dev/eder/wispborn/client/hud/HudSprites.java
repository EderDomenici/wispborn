package dev.eder.wispborn.client.hud;

import dev.eder.wispborn.WispbornMod;
import net.minecraft.resources.ResourceLocation;

public final class HudSprites {
    public static final ResourceLocation CORE_CONTAINER = hud("core_container");
    public static final ResourceLocation CORE_CONTAINER_BLINKING = hud("core_container_blinking");
    public static final ResourceLocation CORE_FULL = hud("core_full");
    public static final ResourceLocation CORE_HALF = hud("core_half");

    public static final ResourceLocation HUNGER_EMPTY = hud("hunger_empty");
    public static final ResourceLocation HUNGER_HALF = hud("hunger_half");
    public static final ResourceLocation HUNGER_FULL = hud("hunger_full");

    public static final ResourceLocation MANA_BAR_BACKGROUND = hud("mana_bar_background");
    public static final ResourceLocation MANA_BAR_PROGRESS = hud("mana_bar_progress");

    public static final ResourceLocation DNA_BAR_BACKGROUND = hud("dna_bar_background");
    public static final ResourceLocation DNA_BAR_PROGRESS = hud("dna_bar_progress");
    public static final ResourceLocation DNA_ICON = hud("dna_icon");
    public static final ResourceLocation DNA_ORB = hud("dna_orb");

    public static final ResourceLocation HOST_FRAME = hud("host_frame");
    public static final ResourceLocation HOST_HEALTH_BACKGROUND = hud("host_health_background");
    public static final ResourceLocation HOST_HEALTH_PROGRESS = hud("host_health_progress");
    public static final ResourceLocation HOST_FLOOR_MARKER = hud("host_floor_marker");
    public static final ResourceLocation CONSUME_READY = hud("consume_ready");
    public static final ResourceLocation CONSUME_BADGE = hud("consume_badge");
    public static final ResourceLocation NO_EP = hud("no_ep");

    public static final ResourceLocation TARGET_FRAME = hud("target_frame");
    public static final ResourceLocation TARGET_OK = hud("target_ok");
    public static final ResourceLocation TARGET_BLOCKED = hud("target_blocked");
    public static final ResourceLocation CAST_BAR_BACKGROUND = hud("cast_bar_background");
    public static final ResourceLocation CAST_BAR_PROGRESS = hud("cast_bar_progress");
    public static final ResourceLocation CAST_VIGNETTE = hud("cast_vignette");

    public static final ResourceLocation ICONS_FONT =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "icons");

    private HudSprites() {
    }

    private static ResourceLocation hud(String name) {
        return ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "hud/" + name);
    }
}
