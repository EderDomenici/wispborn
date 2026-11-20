package dev.eder.wispborn.client.grimoire;

import dev.eder.wispborn.WispbornMod;
import net.minecraft.resources.ResourceLocation;

/**
 * Identificadores de sprites do Grimório no atlas de GUI ({@code textures/gui/sprites/grimoire/<nome>.png}).
 */
public final class GrimoireSprites {
    public static final ResourceLocation PANEL = grimoire("panel");
    public static final ResourceLocation PORTRAIT_FRAME = grimoire("portrait_frame");
    public static final ResourceLocation TAB_UNDERLINE = grimoire("tab_underline");

    public static final ResourceLocation PIP_EMPTY = grimoire("pip_empty");
    public static final ResourceLocation PIP_FILLED = grimoire("pip_filled");
    public static final ResourceLocation PIP_PENDING = grimoire("pip_pending");

    public static final ResourceLocation BUTTON = grimoire("button");
    public static final ResourceLocation BUTTON_HOVER = grimoire("button_hover");
    public static final ResourceLocation BUTTON_DISABLED = grimoire("button_disabled");

    public static final ResourceLocation BUTTON_PRIMARY = grimoire("button_primary");
    public static final ResourceLocation BUTTON_PRIMARY_HOVER = grimoire("button_primary_hover");
    public static final ResourceLocation BUTTON_PRIMARY_DISABLED = grimoire("button_primary_disabled");

    public static final ResourceLocation POINTS_BOX_ACTIVE = grimoire("points_box_active");
    public static final ResourceLocation POINTS_BOX_EMPTY = grimoire("points_box_empty");

    public static final ResourceLocation BAR_BACKGROUND = grimoire("bar_background");
    public static final ResourceLocation BAR_LIFE = grimoire("bar_life");
    public static final ResourceLocation BAR_MANA = grimoire("bar_mana");
    public static final ResourceLocation BAR_HUNGER = grimoire("bar_hunger");
    public static final ResourceLocation BAR_EP = grimoire("bar_ep");

    private GrimoireSprites() {
    }

    private static ResourceLocation grimoire(String name) {
        return ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "grimoire/" + name);
    }
}
