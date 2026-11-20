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

    private static final int COLOR_TEXT_PRIMARY = 0xF3EAD6;
    private static final int COLOR_TEXT_MUTED = 0xB5A88F;
    private static final int COLOR_EP_TOTAL = 0xFFF6D8;
    private static final int COLOR_CONSUME_PROMPT = 0xFFF6D8;
    private static final int COLOR_INFEST_OK = 0xA8C46A;
    private static final int COLOR_INFEST_FAIL = 0xD0563C;

    private static final int DNA_BAR_W = 200;
    private static final int DNA_BAR_H = 6;
    private static final int MANA_BAR_W = 80;
    private static final int MANA_BAR_H = 6;
    private static final int DNA_ORB_SIZE = 19;

    private static final int HOST_FRAME_W = 128;
    private static final int HOST_FRAME_H = 24;
    private static final int HOST_BAR_W = 118;
    private static final int HOST_BAR_H = 5;
    private static final int TARGET_CARD_H = 36;
    private static final int TARGET_CARD_PAD = 6;
    private static final int TARGET_CARD_MIN_W = 72;
    private static final int TARGET_CARD_MAX_W = 160;
    private static final int CONSUME_BADGE_W = 84;
    private static final int CONSUME_BADGE_H = 16;
    private static final int CAST_BAR_W = 132;
    private static final int CAST_BAR_H = 4;
    /** Base do slot central (selo Consumir / barra de conjuração), 2 px acima do texto do EP total (h-43). */
    private static final int BOTTOM_SLOT_OFFSET = 45;

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
            int sx = cx - 100 + col * 8;
            int sy = h - 24 - row * 10;
            graphics.blitSprite(containerSprite, sx, sy, 9, 9);
            if (states[i] == 2) {
                graphics.blitSprite(HudSprites.CORE_FULL, sx, sy, 9, 9);
            } else if (states[i] == 1) {
                graphics.blitSprite(HudSprites.CORE_HALF, sx, sy, 9, 9);
            }
        }
        RenderSystem.disableBlend();

        mc.gui.leftHeight = 34 + (rows - 1) * 10;
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
        int extraRowsShift = 10 * (healthRows(player) - 1);

        RenderSystem.enableBlend();
        for (int i = 0; i < 10; i++) {
            int sx = cx + 20 + i * 8;
            int sy = h - 32 - extraRowsShift;
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

        mc.gui.rightHeight = 42 + extraRowsShift;
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
        int extraRowsShift = 10 * (healthRows(player) - 1);
        int barX = cx + 20;
        int barY = h - 22 - extraRowsShift;

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.MANA_BAR_BACKGROUND, barX, barY, MANA_BAR_W, MANA_BAR_H);
        int fill = HudMath.progressWidth(state.mana(), state.maxMana(), MANA_BAR_W);
        if (fill > 0) {
            graphics.blitSprite(HudSprites.MANA_BAR_PROGRESS, MANA_BAR_W, MANA_BAR_H, 0, 0, barX, barY, fill, MANA_BAR_H);
        }
        RenderSystem.disableBlend();
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
        int barX = cx - 100;
        int barY = h - 14;

        int fill;
        if (state.nextLevelEp() < 0) {
            fill = DNA_BAR_W;
        } else {
            int inLevel = Math.max(0, state.totalEp() - state.levelStartEp());
            int span = Math.max(1, state.nextLevelEp() - state.levelStartEp());
            fill = HudMath.progressWidth(inLevel, span, DNA_BAR_W);
        }

        int extraRowsShift = 10 * (healthRows(player) - 1);

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.DNA_BAR_BACKGROUND, barX, barY, DNA_BAR_W, DNA_BAR_H);
        if (fill > 0) {
            graphics.blitSprite(HudSprites.DNA_BAR_PROGRESS, DNA_BAR_W, DNA_BAR_H, 0, 0, barX, barY, fill, DNA_BAR_H);
        }
        graphics.blitSprite(HudSprites.DNA_ORB, cx - DNA_ORB_SIZE / 2, h - 34 - extraRowsShift, DNA_ORB_SIZE, DNA_ORB_SIZE);
        RenderSystem.disableBlend();

        String totalEpText = Integer.toString(state.totalEp());
        graphics.drawCenteredString(mc.font, totalEpText, cx, h - 43 - extraRowsShift, COLOR_EP_TOTAL);
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
        int frameY = TopSlot.top();
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

    /** Selo "Consumir" no centro inferior, 2 px acima do texto do EP total, só com o hospedeiro no piso. */
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

    /** Barra de conjuração no centro inferior, 2 px acima do texto do EP total, só durante a conjuração. */
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
        int y = graphics.guiHeight() - BOTTOM_SLOT_OFFSET - CAST_BAR_H - 10 * (healthRows(player) - 1);

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.CAST_BAR_BACKGROUND, x, y, CAST_BAR_W, CAST_BAR_H);
        if (castFill > 0) {
            graphics.blitSprite(HudSprites.CAST_BAR_PROGRESS, CAST_BAR_W, CAST_BAR_H, 0, 0, x, y, castFill, CAST_BAR_H);
        }
        RenderSystem.disableBlend();
    }

    /**
     * Cartão do alvo no topo central (opção B, 20/11/2026): nome; selo de sexo + nível; ícone + "Infestar"
     * em verde (pode) ou vermelho (não pode). A largura acompanha a linha mais longa.
     */
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

        Font font = mc.font;
        boolean canInfest = info.result() == InfestResult.OK;
        Component targetName = target instanceof Player
                ? target.getDisplayName()
                : target.getType().getDescription();
        boolean hasTraits = info.mobLevel() > 0 && info.sex().isPresent();
        Component sexGlyph = hasTraits ? info.sex().get().glyphComponent() : Component.empty();
        Component levelText = hasTraits
                ? Component.translatable("wispborn.hud.level", info.mobLevel())
                : Component.empty();
        Component action = Component.translatable("wispborn.target.infest");

        int traitsWidth = hasTraits ? font.width(sexGlyph) + 3 + font.width(levelText) : 0;
        int actionWidth = 9 + 3 + font.width(action);
        int maxNameWidth = TARGET_CARD_MAX_W - TARGET_CARD_PAD * 2;
        String nameStr = ellipsize(font, targetName.getString(), maxNameWidth);
        int widest = Math.max(font.width(nameStr), Math.max(traitsWidth, actionWidth));
        int w = HudMath.cardWidth(widest, TARGET_CARD_PAD, TARGET_CARD_MIN_W, TARGET_CARD_MAX_W);
        int x = graphics.guiWidth() / 2 - w / 2;
        int y = TopSlot.top();
        int textX = x + TARGET_CARD_PAD;

        RenderSystem.enableBlend();
        graphics.blitSprite(HudSprites.TARGET_FRAME, x, y, w, TARGET_CARD_H);
        graphics.blitSprite(canInfest ? HudSprites.TARGET_OK : HudSprites.TARGET_BLOCKED, textX, y + 24, 9, 9);
        RenderSystem.disableBlend();

        graphics.drawString(font, nameStr, textX, y + 5, COLOR_TEXT_PRIMARY, true);
        if (hasTraits) {
            graphics.drawString(font, sexGlyph, textX, y + 14, 0xFFFFFF, false);
            graphics.drawString(font, levelText, textX + font.width(sexGlyph) + 3, y + 15, COLOR_TEXT_MUTED, true);
        }
        graphics.drawString(font, action, textX + 12, y + 25, canInfest ? COLOR_INFEST_OK : COLOR_INFEST_FAIL, true);
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
