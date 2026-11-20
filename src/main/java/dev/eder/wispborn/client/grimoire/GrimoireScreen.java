package dev.eder.wispborn.client.grimoire;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.eder.wispborn.client.ClientWispState;
import dev.eder.wispborn.client.WispRenderHandler;
import dev.eder.wispborn.client.hud.HudMath;
import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.network.AllocateStatsPayload;
import dev.eder.wispborn.network.SyncWispStatePayload;
import dev.eder.wispborn.progress.PendingAllocation;
import dev.eder.wispborn.progress.Stat;
import dev.eder.wispborn.progress.StatMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Tela do Grimório — página do Wisp (paleta B, 100% em {@link GuiGraphics#blitSprite} e {@link Font}).
 */
public final class GrimoireScreen extends Screen {
    public static final int PANEL_WIDTH = 316;
    public static final int PANEL_HEIGHT = 200;

    private static final int COLOR_TEXT_PRIMARY = 0xF3EAD6;
    private static final int COLOR_TEXT_SECONDARY = 0xB5A88F;
    private static final int COLOR_TEXT_DISABLED = 0x6F655A;
    private static final int COLOR_RUST_LIGHT = 0xE2775F;
    private static final int COLOR_POINTS_ACTIVE = 0xFFB9A3;
    private static final int COLOR_CONFIRM_ACTIVE = 0xFFF3D6;

    private static final int EP_BAR_WIDTH = 120;
    /** Vida, mana e fome: mais curtas que a de EP para o valor caber à direita da barra. */
    private static final int STAT_BAR_WIDTH = 90;
    private static final int BARS_RIGHT = 236;
    private static final int STAT_VALUE_X = BARS_RIGHT + 4;
    private static final int BAR_HEIGHT = 5;
    private static final int STAT_TEXT_MAX_WIDTH = 84;
    private static final int STEP_BTN_SIZE = 11;
    private static final int PIP_SIZE = 7;
    private static final int PIP_STEP = 8;
    private static final int PIP_COUNT = 10;
    private static final int ACTION_BTN_WIDTH = 56;
    private static final int ACTION_BTN_HEIGHT = 18;
    private static final int TAB_GAP = 6;

    private static final String[] TAB_KEYS = {
            "wispborn.grimoire.tab.wisp",
            "wispborn.grimoire.tab.traits",
            "wispborn.grimoire.tab.aspects",
            "wispborn.grimoire.tab.skills",
            "wispborn.grimoire.tab.bestiary"
    };

    private final PendingAllocation pending;

    public GrimoireScreen() {
        super(Component.translatable("wispborn.grimoire.title"));
        SyncWispStatePayload state = ClientWispState.get();
        this.pending = new PendingAllocation(state.stats(), state.availablePoints(), state.maxPerStat());
    }

    /** Reaplica os pendentes sobre o novo estado sincronizado pelo servidor (regra 13). */
    public void onStateSynced(SyncWispStatePayload state) {
        this.pending.rebase(state.stats(), state.availablePoints(), state.maxPerStat());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft == null
                || this.minecraft.player == null
                || !this.minecraft.player.isAlive()
                || !FormManager.isParasite(this.minecraft.player)) {
            this.onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Regra de ouro: nenhum fill ou gradiente de fundo; tudo é sprite PNG do mod.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        Player player = this.minecraft.player;
        SyncWispStatePayload state = ClientWispState.get();
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        RenderSystem.enableBlend();
        graphics.blitSprite(GrimoireSprites.PANEL, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        RenderSystem.disableBlend();

        renderHeader(graphics, this.font, left, top);
        renderPortrait(graphics, left, top, mouseX, mouseY, player);
        renderBars(graphics, this.font, left, top, player, state);
        renderAttributes(graphics, this.font, left, top, mouseX, mouseY, state);
        renderFooter(graphics, this.font, left, top, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics graphics, Font font, int left, int top) {
        graphics.drawString(font, this.title, left + 8, top + 7, COLOR_TEXT_PRIMARY, true);

        int tabX = left + 74;
        for (int i = 0; i < TAB_KEYS.length; i++) {
            Component tabLabel = Component.translatable(TAB_KEYS[i]);
            int tabW = font.width(tabLabel);
            boolean active = i == 0;
            graphics.drawString(
                    font,
                    tabLabel,
                    tabX,
                    top + 7,
                    active ? COLOR_TEXT_PRIMARY : COLOR_TEXT_DISABLED,
                    active
            );
            if (active) {
                RenderSystem.enableBlend();
                graphics.blitSprite(GrimoireSprites.TAB_UNDERLINE, tabX, top + 17, tabW, 3);
                RenderSystem.disableBlend();
            }
            tabX += tabW + TAB_GAP;
        }
    }

    private void renderPortrait(GuiGraphics graphics, int left, int top, int mouseX, int mouseY, Player player) {
        RenderSystem.enableBlend();
        graphics.blitSprite(GrimoireSprites.PORTRAIT_FRAME, left + 8, top + 24, 100, 168);
        RenderSystem.disableBlend();

        WispRenderHandler.setRenderingGrimoirePortrait(true);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    left + 12,
                    top + 28,
                    left + 104,
                    top + 188,
                    28,
                    0.0625F,
                    mouseX,
                    mouseY,
                    player
            );
        } finally {
            WispRenderHandler.setRenderingGrimoirePortrait(false);
        }
    }

    private void renderBars(
            GuiGraphics graphics,
            Font font,
            int left,
            int top,
            Player player,
            SyncWispStatePayload state
    ) {
        Component levelLabel = Component.translatable("wispborn.grimoire.level", state.level());
        Component epLabel = state.nextLevelEp() < 0
                ? Component.translatable("wispborn.grimoire.ep_max", state.totalEp())
                : Component.translatable("wispborn.grimoire.ep", state.totalEp(), state.nextLevelEp());
        graphics.drawString(font, levelLabel, left + 116, top + 26, COLOR_TEXT_PRIMARY, true);
        graphics.drawString(font, epLabel, left + BARS_RIGHT - font.width(epLabel), top + 26, COLOR_TEXT_SECONDARY, true);

        int epFill;
        if (state.nextLevelEp() < 0) {
            epFill = EP_BAR_WIDTH;
        } else {
            int inLevel = Math.max(0, state.totalEp() - state.levelStartEp());
            int span = Math.max(1, state.nextLevelEp() - state.levelStartEp());
            epFill = HudMath.progressWidth(inLevel, span, EP_BAR_WIDTH);
        }

        float health = player.getHealth();
        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        int lifeFill = HudMath.progressWidth(health, maxHealth, STAT_BAR_WIDTH);

        float mana = state.mana();
        int maxMana = Math.max(1, state.maxMana());
        int manaFill = HudMath.progressWidth(mana, maxMana, STAT_BAR_WIDTH);

        int food = player.getFoodData().getFoodLevel();
        int hungerFill = HudMath.progressWidth(food, 20.0F, STAT_BAR_WIDTH);

        int remaining = this.pending.remainingPoints();
        boolean hasPoints = remaining > 0;
        ResourceLocation pointsBox = hasPoints
                ? GrimoireSprites.POINTS_BOX_ACTIVE
                : GrimoireSprites.POINTS_BOX_EMPTY;

        RenderSystem.enableBlend();
        drawBar(graphics, GrimoireSprites.BAR_BACKGROUND, GrimoireSprites.BAR_EP, EP_BAR_WIDTH, left + 116, top + 38, epFill);
        drawBar(graphics, GrimoireSprites.BAR_BACKGROUND_SHORT, GrimoireSprites.BAR_LIFE, STAT_BAR_WIDTH, left + 146, top + 49, lifeFill);
        drawBar(graphics, GrimoireSprites.BAR_BACKGROUND_SHORT, GrimoireSprites.BAR_MANA, STAT_BAR_WIDTH, left + 146, top + 59, manaFill);
        drawBar(graphics, GrimoireSprites.BAR_BACKGROUND_SHORT, GrimoireSprites.BAR_HUNGER, STAT_BAR_WIDTH, left + 146, top + 69, hungerFill);
        graphics.blitSprite(pointsBox, left + 270, top + 24, 38, 28);
        RenderSystem.disableBlend();

        graphics.drawString(font, Component.translatable("wispborn.grimoire.life"), left + 116, top + 48, COLOR_TEXT_SECONDARY, true);
        graphics.drawString(font, Component.translatable("wispborn.grimoire.mana"), left + 116, top + 58, COLOR_TEXT_SECONDARY, true);
        graphics.drawString(font, Component.translatable("wispborn.grimoire.hunger"), left + 116, top + 68, COLOR_TEXT_SECONDARY, true);

        int valueX = left + STAT_VALUE_X;
        graphics.drawString(font, Mth.ceil(health) + "/" + Mth.ceil(maxHealth), valueX, top + 48, COLOR_TEXT_PRIMARY, true);
        graphics.drawString(font, Mth.floor(mana) + "/" + state.maxMana(), valueX, top + 58, COLOR_TEXT_PRIMARY, true);
        graphics.drawString(font, food + "/20", valueX, top + 68, COLOR_TEXT_PRIMARY, true);

        String pointsText = "+" + remaining;
        int pointsColor = hasPoints ? COLOR_POINTS_ACTIVE : COLOR_TEXT_DISABLED;
        // Sem legenda: o "+N" basta, e uma legenda sob a caixa colidiria com o valor da mana.
        graphics.drawCenteredString(font, pointsText, left + 289, top + 24 + (28 - font.lineHeight) / 2, pointsColor);
    }

    private void renderAttributes(
            GuiGraphics graphics,
            Font font,
            int left,
            int top,
            int mouseX,
            int mouseY,
            SyncWispStatePayload state
    ) {
        Stat[] stats = Stat.values();
        for (int i = 0; i < stats.length; i++) {
            Stat stat = stats[i];
            int rowY = top + 84 + 22 * i;
            int committed = state.stats().get(stat);
            int total = this.pending.total(stat);
            boolean hasStatPending = this.pending.pending(stat) > 0;

            String statName = ellipsize(font, Component.translatable(stat.translationKey()).getString(), STAT_TEXT_MAX_WIDTH);
            String effectStr = ellipsize(font, formatEffect(stat, state, total, this.pending.pending(stat), this.minecraft.player.getMaxHealth()), STAT_TEXT_MAX_WIDTH);
            graphics.drawString(font, statName, left + 116, rowY, COLOR_TEXT_PRIMARY, true);
            graphics.drawString(
                    font,
                    effectStr,
                    left + 116,
                    rowY + 10,
                    hasStatPending ? COLOR_RUST_LIGHT : COLOR_TEXT_SECONDARY,
                    true
            );

            int minusX = left + 202;
            int btnY = rowY + 3;
            boolean canRemove = this.pending.canRemove(stat);
            boolean minusHover = canRemove && isInside(mouseX, mouseY, minusX, btnY, STEP_BTN_SIZE, STEP_BTN_SIZE);
            ResourceLocation minusSprite = !canRemove
                    ? GrimoireSprites.BUTTON_DISABLED
                    : (minusHover ? GrimoireSprites.BUTTON_HOVER : GrimoireSprites.BUTTON);

            int plusX = left + 297;
            boolean canAdd = this.pending.canAdd(stat);
            boolean plusHover = canAdd && isInside(mouseX, mouseY, plusX, btnY, STEP_BTN_SIZE, STEP_BTN_SIZE);
            ResourceLocation plusSprite = !canAdd
                    ? GrimoireSprites.BUTTON_DISABLED
                    : (plusHover ? GrimoireSprites.BUTTON_HOVER : GrimoireSprites.BUTTON);

            RenderSystem.enableBlend();
            graphics.blitSprite(minusSprite, minusX, btnY, STEP_BTN_SIZE, STEP_BTN_SIZE);
            for (int p = 0; p < PIP_COUNT; p++) {
                ResourceLocation pipSprite;
                if (p < committed) {
                    pipSprite = GrimoireSprites.PIP_FILLED;
                } else if (p < total) {
                    pipSprite = GrimoireSprites.PIP_PENDING;
                } else {
                    pipSprite = GrimoireSprites.PIP_EMPTY;
                }
                graphics.blitSprite(pipSprite, left + 215 + p * PIP_STEP, rowY + 5, PIP_SIZE, PIP_SIZE);
            }
            graphics.blitSprite(plusSprite, plusX, btnY, STEP_BTN_SIZE, STEP_BTN_SIZE);
            RenderSystem.disableBlend();

            graphics.drawString(font, "-", minusX + 3, btnY + 2, canRemove ? COLOR_TEXT_PRIMARY : COLOR_TEXT_DISABLED, false);
            graphics.drawString(font, "+", plusX + 3, btnY + 2, canAdd ? COLOR_TEXT_PRIMARY : COLOR_TEXT_DISABLED, false);
        }
    }

    private void renderFooter(GuiGraphics graphics, Font font, int left, int top, int mouseX, int mouseY) {
        boolean hasPending = this.pending.hasPending();
        int pendingCount = this.pending.toDelta().total();
        Component hint;
        if (!hasPending) {
            hint = Component.translatable("wispborn.grimoire.hint.idle");
        } else if (pendingCount == 1) {
            hint = Component.translatable("wispborn.grimoire.hint.pending_one");
        } else {
            hint = Component.translatable("wispborn.grimoire.hint.pending", pendingCount);
        }
        String hintStr = ellipsize(font, hint.getString(), 70);
        graphics.drawString(font, hintStr, left + 116, top + 181, hasPending ? COLOR_RUST_LIGHT : COLOR_TEXT_SECONDARY, true);

        int cancelX = left + 190;
        int confirmX = left + 252;
        int btnY = top + 176;

        boolean cancelHover = hasPending && isInside(mouseX, mouseY, cancelX, btnY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT);
        boolean confirmHover = hasPending && isInside(mouseX, mouseY, confirmX, btnY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT);

        ResourceLocation cancelSprite = !hasPending
                ? GrimoireSprites.BUTTON_DISABLED
                : (cancelHover ? GrimoireSprites.BUTTON_HOVER : GrimoireSprites.BUTTON);
        ResourceLocation confirmSprite = !hasPending
                ? GrimoireSprites.BUTTON_PRIMARY_DISABLED
                : (confirmHover ? GrimoireSprites.BUTTON_PRIMARY_HOVER : GrimoireSprites.BUTTON_PRIMARY);

        RenderSystem.enableBlend();
        graphics.blitSprite(cancelSprite, cancelX, btnY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT);
        graphics.blitSprite(confirmSprite, confirmX, btnY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT);
        RenderSystem.disableBlend();

        String cancelText = ellipsize(font, Component.translatable("wispborn.grimoire.cancel").getString(), ACTION_BTN_WIDTH - 6);
        String confirmText = ellipsize(font, Component.translatable("wispborn.grimoire.confirm").getString(), ACTION_BTN_WIDTH - 6);

        graphics.drawCenteredString(
                font,
                cancelText,
                cancelX + ACTION_BTN_WIDTH / 2,
                btnY + 5,
                hasPending ? COLOR_TEXT_PRIMARY : COLOR_TEXT_DISABLED
        );
        graphics.drawCenteredString(
                font,
                confirmText,
                confirmX + ACTION_BTN_WIDTH / 2,
                btnY + 5,
                hasPending ? COLOR_CONFIRM_ACTIVE : COLOR_TEXT_DISABLED
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = (this.width - PANEL_WIDTH) / 2;
            int top = (this.height - PANEL_HEIGHT) / 2;

            Stat[] stats = Stat.values();
            for (int i = 0; i < stats.length; i++) {
                Stat stat = stats[i];
                int btnY = top + 84 + 22 * i + 3;
                if (isInside(mouseX, mouseY, left + 202, btnY, STEP_BTN_SIZE, STEP_BTN_SIZE)) {
                    if (this.pending.remove(stat)) {
                        playClickSound();
                    }
                    return true;
                }
                if (isInside(mouseX, mouseY, left + 297, btnY, STEP_BTN_SIZE, STEP_BTN_SIZE)) {
                    if (this.pending.add(stat)) {
                        playClickSound();
                    }
                    return true;
                }
            }

            int footerY = top + 176;
            if (isInside(mouseX, mouseY, left + 190, footerY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT)) {
                if (this.pending.hasPending()) {
                    this.pending.clear();
                    playClickSound();
                }
                return true;
            }
            if (isInside(mouseX, mouseY, left + 252, footerY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT)) {
                if (this.pending.hasPending()) {
                    PacketDistributor.sendToServer(new AllocateStatsPayload(this.pending.toDelta()));
                    this.pending.clear();
                    playClickSound();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void drawBar(
            GuiGraphics graphics,
            ResourceLocation backgroundSprite,
            ResourceLocation fillSprite,
            int width,
            int x,
            int y,
            int fillWidth
    ) {
        graphics.blitSprite(backgroundSprite, x, y, width, BAR_HEIGHT);
        if (fillWidth > 0) {
            graphics.blitSprite(fillSprite, width, BAR_HEIGHT, 0, 0, x, y, fillWidth, BAR_HEIGHT);
        }
    }

    private static String formatEffect(Stat stat, SyncWispStatePayload state, int totalPoints, int pendingPoints, float maxHealth) {
        return switch (stat) {
            case ABSORPTION -> {
                double pct = StatMath.drainPercent(
                        WispbornConfig.DRAIN_BASE_PERCENT_PER_PULSE.getAsDouble(),
                        totalPoints,
                        WispbornConfig.ABSORPTION_PER_POINT.getAsDouble()
                ) * 100.0D;
                yield Component.translatable("wispborn.grimoire.effect.absorption", formatOneDecimal(pct)).getString();
            }
            case EFFICIENCY -> {
                double mult = StatMath.epMultiplier(
                        totalPoints,
                        WispbornConfig.EFFICIENCY_PER_POINT.getAsDouble()
                );
                yield Component.translatable("wispborn.grimoire.effect.efficiency", formatOneDecimal(mult)).getString();
            }
            case RESERVE -> {
                int maxMana = StatMath.maxMana(
                        WispbornConfig.MANA_BASE.getAsInt(),
                        state.level(),
                        WispbornConfig.MANA_PER_LEVEL.getAsInt(),
                        totalPoints,
                        WispbornConfig.RESERVE_PER_POINT.getAsInt()
                );
                yield Component.translatable("wispborn.grimoire.effect.reserve", maxMana).getString();
            }
            case VITALITY -> {
                // Núcleos atuais vêm da vida máxima real (já inclui a Vitalidade gasta); soma só os pendentes.
                int cores = Mth.ceil(maxHealth / 2.0F)
                        + pendingPoints * WispbornConfig.VITALITY_HEARTS_PER_POINT.getAsInt();
                yield Component.translatable("wispborn.grimoire.effect.vitality", cores).getString();
            }
        };
    }

    private static String formatOneDecimal(double value) {
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getLanguageManager().getSelected().startsWith("pt_")) {
            return formatted.replace('.', ',');
        }
        return formatted;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
