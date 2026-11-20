package dev.eder.wispborn.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.client.ClientPossession;
import dev.eder.wispborn.client.ClientWispState;
import dev.eder.wispborn.client.KeyBindings;
import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.host.DrainMath;
import dev.eder.wispborn.host.HostManager;
import dev.eder.wispborn.host.InfestResult;
import dev.eder.wispborn.network.CastStatePayload;
import dev.eder.wispborn.network.SyncWispStatePayload;
import dev.eder.wispborn.network.TargetInfoPayload;
import dev.eder.wispborn.traits.MobTraits;
import dev.eder.wispborn.traits.MobTraitsManager;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class WispHud {
    private static final ResourceLocation WISP_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_health");
    private static final ResourceLocation WISP_HUNGER_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_hunger");
    private static final ResourceLocation WISP_MANA_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_mana");
    private static final ResourceLocation WISP_DNA_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp_dna");
    private static final ResourceLocation HOST_PANEL_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "host_panel");
    private static final ResourceLocation POSSESS_TARGET_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "possess_target");

    private static final Set<ResourceLocation> HIDDEN_VANILLA_LAYERS = Set.of(
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.SELECTED_ITEM_NAME,
            VanillaGuiLayers.VEHICLE_HEALTH,
            VanillaGuiLayers.JUMP_METER
    );

    private static final int COLOR_TEXT_PRIMARY = 0xE6F7F2;
    private static final int COLOR_MANA_TEXT = 0xD0CAFF;
    private static final int COLOR_POINTS_TEXT = 0x8BE8BD;
    private static final int COLOR_CONSUME_WAIT = 0xE4CBA6;
    private static final int COLOR_TARGET_OK = 0xA1EFD6;
    private static final int COLOR_TARGET_FAIL = 0xFF7B7B;

    private static final RandomSource RANDOM = RandomSource.create();
    private static int lastHealth = 0;
    private static long lastHealthTime = 0L;
    private static long healthBlinkTime = 0L;

    private WispHud() {
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, WISP_HEALTH_ID, WispHud::renderWispHealth);
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, WISP_HUNGER_ID, WispHud::renderWispHunger);
        event.registerAbove(VanillaGuiLayers.HOTBAR, WISP_MANA_ID, WispHud::renderWispMana);
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, WISP_DNA_ID, WispHud::renderWispDna);
        event.registerAbove(VanillaGuiLayers.VEHICLE_HEALTH, HOST_PANEL_ID, WispHud::renderHostPanel);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, POSSESS_TARGET_ID, WispHud::renderPossessTarget);
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isSpectator() && FormManager.isParasite(mc.player)) {
            if (HIDDEN_VANILLA_LAYERS.contains(event.getName())) {
                event.setCanceled(true);
            }
        }
    }

    private static void renderWispHealth(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = activeParasitePlayer(mc);
        if (player == null) {
            return;
        }

        int currentHealth = Mth.ceil(player.getHealth());
        long guiTicks = mc.gui.getGuiTicks();
        boolean blink = healthBlinkTime > guiTicks && ((healthBlinkTime - guiTicks) / 3L) % 2L == 1L;
        long nowMs = Util.getMillis();
        if (currentHealth < lastHealth && player.invulnerableTime > 0) {
            lastHealthTime = nowMs;
            healthBlinkTime = guiTicks + 20L;
        } else if (currentHealth > lastHealth && player.invulnerableTime > 0) {
            lastHealthTime = nowMs;
            healthBlinkTime = guiTicks + 10L;
        }
        if (nowMs - lastHealthTime > 1000L) {
            lastHealthTime = nowMs;
        }
        lastHealth = currentHealth;

        float maxHealth = Math.max(player.getMaxHealth(), player.getHealth());
        int totalIcons = Math.max(1, Mth.ceil(maxHealth / 2.0F));
        int rows = healthRows(player);
        int[] states = HudMath.iconStates(player.getHealth(), totalIcons);

        int cx = graphics.guiWidth() / 2;
        int h = graphics.guiHeight();
        ResourceLocation containerSprite = blink ? HudSprites.CORE_CONTAINER_BLINKING : HudSprites.CORE_CONTAINER;

        RenderSystem.enableBlend();
        for (int i = 0; i < totalIcons; i++) {
            int row = i / 10;
            int col = i % 10;
            int sx = cx - 91 + col * 8;
            int sy = h - 44 - row * 10;
            graphics.blitSprite(containerSprite, sx, sy, 9, 9);
            if (states[i] == 2) {
                graphics.blitSprite(HudSprites.CORE_FULL, sx, sy, 9, 9);
            } else if (states[i] == 1) {
                graphics.blitSprite(HudSprites.CORE_HALF, sx, sy, 9, 9);
            }
        }
        RenderSystem.disableBlend();

        mc.gui.leftHeight = 54 + (rows - 1) * 10;
    }

    private static void renderWispHunger(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = activeParasitePlayer(mc);
        if (player == null) {
            return;
        }

        int foodLevel = player.getFoodData().getFoodLevel();
        float saturation = player.getFoodData().getSaturationLevel();
        int guiTicks = mc.gui.getGuiTicks();
        int[] states = HudMath.iconStates(foodLevel, 10);
        RANDOM.setSeed(guiTicks * 312871L);

        int cx = graphics.guiWidth() / 2;
        int h = graphics.guiHeight();

        RenderSystem.enableBlend();
        for (int i = 0; i < 10; i++) {
            int sx = cx + 11 + i * 8;
            int sy = h - 44;
            if (saturation <= 0.0F && guiTicks % (foodLevel * 3 + 1) == 0) {
                sy += RANDOM.nextInt(3) - 1;
            }
            int slotState = states[9 - i];
            ResourceLocation sprite = switch (slotState) {
                case 2 -> HudSprites.HUNGER_FULL;
                case 1 -> HudSprites.HUNGER_HALF;
                default -> HudSprites.HUNGER_EMPTY;
            };
            graphics.blitSprite(sprite, sx, sy, 9, 9);
        }
        RenderSystem.disableBlend();

        mc.gui.rightHeight = 54;
    }

    private static void renderWispMana(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = activeParasitePlayer(mc);
        if (player == null) {
            return;
        }

        SyncWispStatePayload state = ClientWispState.get();
        int cx = graphics.guiWidth() / 2;
        int h = graphics.guiHeight();
        int barX = cx - 91;
        int barY = h - 12;

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.MANA_BAR_BACKGROUND, barX, barY, 182, 9);
        int fill = HudMath.progressWidth(state.mana(), state.maxMana(), 182);
        if (fill > 0) {
            graphics.blitSprite(HudSprites.MANA_BAR_PROGRESS, 182, 9, 0, 0, barX, barY, fill, 9);
        }
        RenderSystem.disableBlend();

        Component manaText = Component.translatable("wispborn.hud.mana", state.mana(), state.maxMana());
        graphics.drawCenteredString(mc.font, manaText, cx, h - 21, COLOR_MANA_TEXT);
    }

    private static void renderWispDna(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = activeParasitePlayer(mc);
        if (player == null) {
            return;
        }

        SyncWispStatePayload state = ClientWispState.get();
        int cx = graphics.guiWidth() / 2;
        int h = graphics.guiHeight();
        int barX = cx - 91;
        int barY = h - 30;

        int fill;
        if (state.nextLevelEp() < 0) {
            fill = 182;
        } else {
            int inLevel = Math.max(0, state.totalEp() - state.levelStartEp());
            int span = Math.max(1, state.nextLevelEp() - state.levelStartEp());
            fill = HudMath.progressWidth(inLevel, span, 182);
        }

        int extraRowsShift = 10 * (healthRows(player) - 1);

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.DNA_BAR_BACKGROUND, barX, barY, 182, 7);
        if (fill > 0) {
            graphics.blitSprite(HudSprites.DNA_BAR_PROGRESS, 182, 7, 0, 0, barX, barY, fill, 7);
        }
        graphics.blitSprite(HudSprites.DNA_ICON, cx - 27, h - 54 - extraRowsShift, 9, 9);
        RenderSystem.disableBlend();

        Font font = mc.font;
        Component levelText = Component.translatable("wispborn.hud.level", state.level());
        graphics.drawString(font, levelText, cx - 14, h - 52 - extraRowsShift, COLOR_TEXT_PRIMARY, true);

        if (state.availablePoints() > 0) {
            Component ptsText = Component.translatable("wispborn.hud.stat_points", state.availablePoints());
            graphics.drawString(font, ptsText, cx + 38, h - 52 - extraRowsShift, COLOR_POINTS_TEXT, true);
        }
    }

    private static void renderHostPanel(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = activeParasitePlayer(mc);
        if (player == null || !HostManager.isHost(player)) {
            return;
        }

        Optional<LivingEntity> hostOpt = HostManager.hostOf(player);
        if (hostOpt.isEmpty() || !hostOpt.get().isAlive()) {
            return;
        }

        LivingEntity host = hostOpt.get();
        SyncWispStatePayload state = ClientWispState.get();
        int cx = graphics.guiWidth() / 2;
        int h = graphics.guiHeight();
        int yShift = 10 * (healthRows(player) - 1);
        if (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply()) {
            yShift = Math.max(yShift, 10);
        }

        double floorPercent = WispbornConfig.DRAIN_FLOOR_PERCENT.getAsDouble();
        boolean consumeReady = state.hostConsumeReady()
                || DrainMath.atFloor(host.getHealth(), host.getMaxHealth(), floorPercent);
        boolean noEp = state.hostEpFactor() <= 0.0F;

        int barX = cx - 82;
        int barY = h - 80 - yShift;
        int hpFill = HudMath.progressWidth(host.getHealth(), host.getMaxHealth(), 164);
        int markerOffset = HudMath.floorMarkerX(floorPercent, 164);

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.HOST_FRAME, cx - 91, h - 99 - yShift, 182, 44);
        graphics.blitSprite(HudSprites.HOST_HEALTH_BACKGROUND, barX, barY, 164, 6);
        if (hpFill > 0) {
            graphics.blitSprite(HudSprites.HOST_HEALTH_PROGRESS, 164, 6, 0, 0, barX, barY, hpFill, 6);
        }
        graphics.blitSprite(HudSprites.HOST_FLOOR_MARKER, barX + markerOffset - 1, h - 81 - yShift, 3, 8);
        if (consumeReady) {
            graphics.blitSprite(HudSprites.CONSUME_READY, cx - 82, h - 69 - yShift, 9, 9);
        }
        if (noEp) {
            graphics.blitSprite(HudSprites.NO_EP, cx + 70, h - 69 - yShift, 9, 9);
        }
        RenderSystem.disableBlend();

        Font font = mc.font;
        int topRowY = h - 93 - yShift;
        int healthPct = Math.round((host.getHealth() / Math.max(1.0F, host.getMaxHealth())) * 100.0F);
        String pctText = healthPct + "%";
        int pctX = cx + 82 - font.width(pctText);
        graphics.drawString(font, pctText, pctX, topRowY, COLOR_TEXT_PRIMARY, true);

        Optional<MobTraits> traitsOpt = (host instanceof Mob mob)
                ? MobTraitsManager.traitsOf(mob)
                : Optional.empty();
        Component sexGlyph = traitsOpt.map(t -> t.sex().glyphComponent()).orElse(Component.empty());
        Component levelText = traitsOpt
                .map(t -> (Component) Component.translatable("wispborn.hud.level", t.level()))
                .orElse(Component.empty());
        int suffixWidth = traitsOpt.isPresent()
                ? (5 + font.width(sexGlyph) + 3 + font.width(levelText))
                : 0;
        int nameX = cx - 82;
        int maxNameWidth = Math.max(16, (pctX - 6) - nameX - suffixWidth);
        String nameStr = ellipsize(font, host.getType().getDescription().getString(), maxNameWidth);
        graphics.drawString(font, nameStr, nameX, topRowY, COLOR_TEXT_PRIMARY, true);

        if (traitsOpt.isPresent()) {
            int glyphX = nameX + font.width(nameStr) + 5;
            graphics.drawString(font, sexGlyph, glyphX, topRowY, 0xFFFFFF, true);
            int levelX = glyphX + font.width(sexGlyph) + 3;
            graphics.drawString(font, levelText, levelX, topRowY, COLOR_TEXT_PRIMARY, true);
        }

        int bottomRowY = h - 68 - yShift;
        int statusX = consumeReady ? (cx - 82 + 12) : (cx - 82);
        int statusRightLimit = noEp ? (cx + 68) : (cx + 82);
        Component statusComp = consumeReady
                ? Component.translatable("wispborn.hud.host.consume_ready", KeyBindings.CONSUME.getTranslatedKeyMessage())
                : Component.translatable("wispborn.hud.host.consume_wait", Math.round(floorPercent * 100.0D));
        String statusStr = ellipsize(font, statusComp.getString(), Math.max(16, statusRightLimit - statusX));
        int statusColor = consumeReady ? COLOR_POINTS_TEXT : COLOR_CONSUME_WAIT;
        graphics.drawString(font, statusStr, statusX, bottomRowY, statusColor, true);
    }

    private static void renderPossessTarget(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = activeParasitePlayer(mc);
        if (player == null || mc.level == null || !FormManager.isWisp(player)) {
            return;
        }

        TargetInfoPayload info = ClientPossession.targetInfo();
        if (info.entityId() == -1) {
            return;
        }

        Entity target = mc.level.getEntity(info.entityId());
        if (target == null || !target.isAlive()) {
            return;
        }

        int cx = graphics.guiWidth() / 2;
        int h = graphics.guiHeight();
        int frameX = cx - 75;
        int frameY = Math.max(4, h - (h / 2 + 53));

        CastStatePayload cast = ClientPossession.castState();
        int castFill = 0;
        if (cast.targetEntityId() == info.entityId() && cast.durationTicks() > 0) {
            float elapsed = (mc.level.getGameTime() - ClientPossession.castStartGameTime())
                    + deltaTracker.getGameTimeDeltaPartialTick(false);
            castFill = HudMath.progressWidth(elapsed, cast.durationTicks(), 132);
        }

        ResourceLocation statusIcon = info.result() == InfestResult.OK
                ? HudSprites.TARGET_OK
                : HudSprites.TARGET_BLOCKED;

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.TARGET_FRAME, frameX, frameY, 150, 32);
        graphics.blitSprite(statusIcon, cx + 55, frameY + 4, 9, 9);
        graphics.blitSprite(HudSprites.CAST_BAR_BACKGROUND, cx - 66, frameY + 24, 132, 4);
        if (castFill > 0) {
            graphics.blitSprite(HudSprites.CAST_BAR_PROGRESS, 132, 4, 0, 0, cx - 66, frameY + 24, castFill, 4);
        }
        RenderSystem.disableBlend();

        Font font = mc.font;
        int topRowY = frameY + 6;
        int nameX = cx - 66;
        int maxRight = cx + 52;
        Component targetName = target instanceof Player
                ? target.getDisplayName()
                : target.getType().getDescription();

        if (info.mobLevel() > 0 && info.sex().isPresent()) {
            Component sexGlyph = info.sex().get().glyphComponent();
            Component levelText = Component.translatable("wispborn.hud.level", info.mobLevel());
            int suffixWidth = 5 + font.width(sexGlyph) + 3 + font.width(levelText);
            int maxNameWidth = Math.max(16, maxRight - nameX - suffixWidth);
            String nameStr = ellipsize(font, targetName.getString(), maxNameWidth);
            graphics.drawString(font, nameStr, nameX, topRowY, COLOR_TEXT_PRIMARY, true);

            int glyphX = nameX + font.width(nameStr) + 5;
            graphics.drawString(font, sexGlyph, glyphX, topRowY, 0xFFFFFF, true);
            int levelX = glyphX + font.width(sexGlyph) + 3;
            graphics.drawString(font, levelText, levelX, topRowY, COLOR_TEXT_PRIMARY, true);
        } else {
            String nameStr = ellipsize(font, targetName.getString(), Math.max(16, maxRight - nameX));
            graphics.drawString(font, nameStr, nameX, topRowY, COLOR_TEXT_PRIMARY, true);
        }

        Component actionComp = buildTargetActionLine(info);
        String actionStr = ellipsize(font, actionComp.getString(), 132);
        int actionColor = info.result() == InfestResult.OK ? COLOR_TARGET_OK : COLOR_TARGET_FAIL;
        graphics.drawString(font, actionStr, cx - 66, frameY + 16, actionColor, true);
    }

    private static Component buildTargetActionLine(TargetInfoPayload info) {
        return switch (info.result()) {
            case OK -> Component.translatable(
                    "wispborn.target.ready_cost",
                    KeyBindings.POSSESS.getTranslatedKeyMessage(),
                    info.manaCost()
            );
            case IMMUNE, NOT_A_MOB -> Component.translatable("wispborn.infest.immune");
            case HAS_PASSENGER -> Component.translatable("wispborn.infest.has_passenger");
            case TOO_STRONG -> Component.translatable("wispborn.infest.too_strong", info.mobLevel(), info.wispLevel());
            case NO_MANA -> Component.translatable("wispborn.infest.no_mana", info.manaCost());
        };
    }

    private static Player activeParasitePlayer(Minecraft mc) {
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator() || !FormManager.isParasite(mc.player)) {
            return null;
        }
        return mc.player;
    }

    private static int healthRows(Player player) {
        float maxHealth = Math.max(player.getMaxHealth(), player.getHealth());
        int totalIcons = Math.max(1, Mth.ceil(maxHealth / 2.0F));
        return Math.max(1, Mth.ceil(totalIcons / 10.0F));
    }

    private static String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int available = Math.max(0, maxWidth - font.width(ellipsis));
        return font.plainSubstrByWidth(text, available) + ellipsis;
    }
}
