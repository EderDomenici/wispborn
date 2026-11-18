package dev.eder.wispborn.progress;

import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.OptionalInt;

public final class ProgressManager {
    private ProgressManager() {
    }

    /** Nível atual, sempre derivado do EP total e da curva do config. */
    public static int level(ServerPlayer player) {
        return LevelCurve.levelFor(player.getData(ModAttachments.PROGRESS).totalEp(), WispbornConfig.levelThresholds());
    }

    public static int availablePoints(ServerPlayer player) {
        return StatMath.availablePoints(
                level(player),
                WispbornConfig.POINTS_PER_LEVEL.get(),
                player.getData(ModAttachments.STATS).spent()
        );
    }

    public static int addEp(ServerPlayer player, int baseEp) {
        if (baseEp <= 0) {
            return 0;
        }

        WispStats stats = player.getData(ModAttachments.STATS);
        double multiplier = StatMath.epMultiplier(stats.efficiency(), WispbornConfig.EFFICIENCY_PER_POINT.get());
        int gainedEp = StatMath.applyEp(baseEp, multiplier);

        WispProgress current = player.getData(ModAttachments.PROGRESS);
        int newTotalEp = Math.max(0, current.totalEp() + gainedEp);
        List<Integer> thresholds = WispbornConfig.levelThresholds();
        int oldLevel = LevelCurve.levelFor(current.totalEp(), thresholds);
        int newLevel = LevelCurve.levelFor(newTotalEp, thresholds);

        player.setData(ModAttachments.PROGRESS, new WispProgress(newTotalEp));

        if (newLevel > oldLevel) {
            int pointsGained = (newLevel - oldLevel) * WispbornConfig.POINTS_PER_LEVEL.get();
            player.sendSystemMessage(Component.translatable("wispborn.level_up", newLevel, pointsGained));

            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        return gainedEp;
    }

    public static void setEp(ServerPlayer player, int total) {
        player.setData(ModAttachments.PROGRESS, new WispProgress(Math.max(0, total)));
        validateStats(player);
    }

    /**
     * Garante que nunca haja pontos negativos: se o nível caiu (por {@code ep set} ou por mudança na curva
     * do config), zera os atributos e avisa o jogador.
     */
    public static void validateStats(ServerPlayer player) {
        if (availablePoints(player) < 0) {
            resetStats(player);
            player.sendSystemMessage(Component.translatable("wispborn.stat.auto_reset"));
        }
    }

    public static AllocateResult allocate(ServerPlayer player, Stat stat) {
        WispStats stats = player.getData(ModAttachments.STATS);
        int currentPoints = stats.get(stat);
        if (currentPoints >= WispbornConfig.MAX_PER_STAT.get()) {
            return AllocateResult.MAXED;
        }

        if (availablePoints(player) <= 0) {
            return AllocateResult.NO_POINTS;
        }

        player.setData(ModAttachments.STATS, stats.with(stat, currentPoints + 1));
        refreshStats(player);
        return AllocateResult.OK;
    }

    public static void resetStats(ServerPlayer player) {
        player.setData(ModAttachments.STATS, WispStats.ZERO);
        refreshStats(player);
    }

    public static void refreshStats(ServerPlayer player) {
        float previousMaxHealth = player.getMaxHealth();
        FormManager.applyModifiers(player);
        if (player.getMaxHealth() > previousMaxHealth && player.getHealth() > 0.0F) {
            float healthDelta = player.getMaxHealth() - previousMaxHealth;
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healthDelta));
        }

        int maxMana = getMaxMana(player);
        float currentMana = getMana(player);
        if (currentMana > maxMana) {
            setMana(player, maxMana);
        }
    }

    public static int getMaxMana(ServerPlayer player) {
        WispStats stats = player.getData(ModAttachments.STATS);
        return StatMath.maxMana(
                WispbornConfig.MANA_BASE.get(),
                stats.reserve(),
                WispbornConfig.RESERVE_PER_POINT.get()
        );
    }

    public static float getMana(ServerPlayer player) {
        return player.getData(ModAttachments.MANA);
    }

    public static void setMana(ServerPlayer player, float mana) {
        int maxMana = getMaxMana(player);
        player.setData(ModAttachments.MANA, Math.clamp(mana, 0.0F, (float) maxMana));
    }

    public static void addMana(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        setMana(player, getMana(player) + amount);
    }

    public static boolean spendMana(ServerPlayer player, float cost) {
        float current = getMana(player);
        if (current < cost) {
            return false;
        }
        setMana(player, current - cost);
        return true;
    }

    /** EP para exibição: {@code 450/800}, ou {@code 18450 · máx} no nível máximo. */
    public static Component epDisplay(ServerPlayer player) {
        int totalEp = player.getData(ModAttachments.PROGRESS).totalEp();
        OptionalInt nextThreshold = LevelCurve.nextThreshold(totalEp, WispbornConfig.levelThresholds());
        return nextThreshold.isPresent()
                ? Component.literal(totalEp + "/" + nextThreshold.getAsInt())
                : Component.translatable("wispborn.hud.max", totalEp);
    }

    public static Component buildWispHud(ServerPlayer player) {
        MutableComponent hud = Component.translatable(
                "wispborn.hud.wisp",
                level(player),
                epDisplay(player),
                Math.round(getMana(player)),
                getMaxMana(player)
        );

        int availablePoints = availablePoints(player);
        if (availablePoints > 0) {
            hud.append(Component.translatable("wispborn.hud.points", availablePoints));
        }

        return hud;
    }
}
