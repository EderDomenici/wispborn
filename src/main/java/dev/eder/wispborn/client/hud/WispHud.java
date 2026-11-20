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
    private static final ResourceLocation CONSUME_PROMPT_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "consume_prompt");
    private static final ResourceLocation CAST_BAR_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "cast_bar");

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
    private static final int COLOR_CONSUME_PROMPT = 0xC7FFE8;
    private static final int COLOR_TARGET_OK = 0xA1EFD6;
    private static final int COLOR_TARGET_FAIL = 0xFF7B7B;

    /** Layout revisado em 20/11/2026: painel do hospedeiro no topo, selo e conjuração no centro inferior. */
    private static final int HOST_FRAME_W = 128;
    private static final int HOST_FRAME_H = 24;
    private static final int HOST_FRAME_TOP = 4;
    private static final int HOST_BAR_W = 118;
    private static final int HOST_BAR_H = 5;
    private static final int TARGET_FRAME_H = 26;
    private static final int CONSUME_BADGE_W = 84;
    private static final int CONSUME_BADGE_H = 16;
    private static final int CAST_BAR_W = 132;
    private static final int CAST_BAR_H = 4;
    /** Topo da linha do nível (DNA + "Nv") medido da base da tela; o slot central fica logo acima. */
    private static final int BOTTOM_SLOT_OFFSET = 56;

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
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, CONSUME_PROMPT_ID, WispHud::renderConsumePrompt);
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, CAST_BAR_ID, WispHud::renderCastBar);
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

    /** Painel compacto do hospedeiro no topo central: nome, sexo, nível, % e barra com a marca do piso. */
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
        int frameX = cx - HOST_FRAME_W / 2;
        int frameY = HOST_FRAME_TOP;
        int barX = frameX + 5;
        int barY = frameY + 15;

        double floorPercent = WispbornConfig.DRAIN_FLOOR_PERCENT.getAsDouble();
        boolean noEp = state.hostEpFactor() <= 0.0F;
        int hpFill = HudMath.progressWidth(host.getHealth(), host.getMaxHealth(), HOST_BAR_W);
        int markerOffset = HudMath.floorMarkerX(floorPercent, HOST_BAR_W);

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.HOST_FRAME, frameX, frameY, HOST_FRAME_W, HOST_FRAME_H);
        graphics.blitSprite(HudSprites.HOST_HEALTH_BACKGROUND, barX, barY, HOST_BAR_W, HOST_BAR_H);
        if (hpFill > 0) {
            graphics.blitSprite(HudSprites.HOST_HEALTH_PROGRESS, HOST_BAR_W, HOST_BAR_H, 0, 0,
                    barX, barY, hpFill, HOST_BAR_H);
        }
        graphics.blitSprite(HudSprites.HOST_FLOOR_MARKER, barX + markerOffset - 1, barY - 2, 3, 8);
        RenderSystem.disableBlend();

        Font font = mc.font;
        int textY = frameY + 4;
        int healthPct = Math.round((host.getHealth() / Math.max(1.0F, host.getMaxHealth())) * 100.0F);
        String pctText = healthPct + "%";
        int pctX = frameX + HOST_FRAME_W - 5 - font.width(pctText);
        graphics.drawString(font, pctText, pctX, textY, COLOR_TEXT_PRIMARY, true);
        int nameRight = pctX - 4;
        if (noEp) {
            RenderSystem.enableBlend();
            graphics.blitSprite(HudSprites.NO_EP, pctX - 11, textY - 1, 9, 9);
            RenderSystem.disableBlend();
            nameRight -= 11;
        }

        Optional<MobTraits> traitsOpt = (host instanceof Mob mob)
                ? MobTraitsManager.traitsOf(mob)
                : Optional.empty();
        Component sexGlyph = traitsOpt.map(t -> t.sex().glyphComponent()).orElse(Component.empty());
        Component levelText = traitsOpt
                .map(t -> (Component) Component.translatable("wispborn.hud.level", t.level()))
                .orElse(Component.empty());
        int suffixWidth = traitsOpt.isPresent() ? (3 + font.width(sexGlyph) + 2 + font.width(levelText)) : 0;
        int nameX = frameX + 5;
        String nameStr = ellipsize(font, host.getType().getDescription().getString(),
                Math.max(16, nameRight - nameX - suffixWidth));
        graphics.drawString(font, nameStr, nameX, textY, COLOR_TEXT_PRIMARY, true);
        if (traitsOpt.isPresent()) {
            int glyphX = nameX + font.width(nameStr) + 3;
            graphics.drawString(font, sexGlyph, glyphX, textY - 1, 0xFFFFFF, false);
            graphics.drawString(font, levelText, glyphX + font.width(sexGlyph) + 2, textY, COLOR_TEXT_PRIMARY, true);
        }
    }

    /** Selo "Consumir" no centro inferior, acima da linha do nível, só com o hospedeiro no piso. */
    private static void renderConsumePrompt(GuiGraphics graphics, DeltaTracker deltaTracker) {
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
        boolean ready = ClientWispState.get().hostConsumeReady()
                || DrainMath.atFloor(host.getHealth(), host.getMaxHealth(),
                        WispbornConfig.DRAIN_FLOOR_PERCENT.getAsDouble());
        if (!ready) {
            return;
        }

        int cx = graphics.guiWidth() / 2;
        int x = cx - CONSUME_BADGE_W / 2;
        int y = graphics.guiHeight() - BOTTOM_SLOT_OFFSET - CONSUME_BADGE_H - 10 * (healthRows(player) - 1);

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.CONSUME_BADGE, x, y, CONSUME_BADGE_W, CONSUME_BADGE_H);
        graphics.blitSprite(HudSprites.CONSUME_READY, x + 9, y + 4, 9, 9);
        RenderSystem.disableBlend();

        Font font = mc.font;
        Component prompt = Component.translatable("wispborn.hud.consume_prompt",
                KeyBindings.CONSUME.getTranslatedKeyMessage());
        int textX = x + 21;
        String promptStr = ellipsize(font, prompt.getString(), CONSUME_BADGE_W - 21 - 9);
        graphics.drawString(font, promptStr, textX, y + 5, COLOR_CONSUME_PROMPT, true);
    }

    /** Barra de conjuração no centro inferior, só enquanto a possessão está sendo conjurada. */
    private static void renderCastBar(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = activeParasitePlayer(mc);
        if (player == null || mc.level == null || !FormManager.isWisp(player) || !ClientPossession.isCasting()) {
            return;
        }
        CastStatePayload cast = ClientPossession.castState();
        if (cast.durationTicks() <= 0) {
            return;
        }
        float elapsed = (mc.level.getGameTime() - ClientPossession.castStartGameTime())
                + deltaTracker.getGameTimeDeltaPartialTick(false);
        int castFill = HudMath.progressWidth(elapsed, cast.durationTicks(), CAST_BAR_W);

        int cx = graphics.guiWidth() / 2;
        int x = cx - CAST_BAR_W / 2;
        int y = graphics.guiHeight() - BOTTOM_SLOT_OFFSET - 8 - 10 * (healthRows(player) - 1);

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.CAST_BAR_BACKGROUND, x, y, CAST_BAR_W, CAST_BAR_H);
        if (castFill > 0) {
            graphics.blitSprite(HudSprites.CAST_BAR_PROGRESS, CAST_BAR_W, CAST_BAR_H, 0, 0, x, y, castFill, CAST_BAR_H);
        }
        RenderSystem.disableBlend();
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
        // A moldura termina 21 px acima da mira, como no LAYOUT.
        int frameY = Math.max(4, h / 2 - 21 - TARGET_FRAME_H);

        ResourceLocation statusIcon = info.result() == InfestResult.OK
                ? HudSprites.TARGET_OK
                : HudSprites.TARGET_BLOCKED;

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.TARGET_FRAME, frameX, frameY, 150, TARGET_FRAME_H);
        graphics.blitSprite(statusIcon, cx + 55, frameY + 4, 9, 9);
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
            graphics.drawString(font, sexGlyph, glyphX, topRowY - 1, 0xFFFFFF, false);
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
