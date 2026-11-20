package dev.eder.wispborn.progress;

import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.host.DrainMath;
import dev.eder.wispborn.host.HostManager;
import dev.eder.wispborn.network.SyncWispStatePayload;
import dev.eder.wispborn.registry.ModAttachments;
import dev.eder.wispborn.traits.MobTraits;
import dev.eder.wispborn.traits.MobTraitsManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressManager {
    private static final Map<UUID, SyncWispStatePayload> LAST_SYNCED_STATE = new ConcurrentHashMap<>();

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

        syncState(player, false);
        return gainedEp;
    }

    public static void setEp(ServerPlayer player, int total) {
        player.setData(ModAttachments.PROGRESS, new WispProgress(Math.max(0, total)));
        validateStats(player);
        syncState(player, false);
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
        syncState(player, false);
        return AllocateResult.OK;
    }

    public static BatchAllocateResult allocateBatch(ServerPlayer player, StatDelta delta) {
        if (!FormManager.isParasite(player)) {
            return BatchAllocateResult.EMPTY;
        }

        WispStats current = player.getData(ModAttachments.STATS);
        int available = availablePoints(player);
        int maxPerStat = WispbornConfig.MAX_PER_STAT.get();
        BatchAllocateResult result = AllocationRules.check(available, current, delta, maxPerStat);
        if (result != BatchAllocateResult.OK) {
            String key = "wispborn.grimoire.allocate." + result.name().toLowerCase(Locale.ROOT);
            player.displayClientMessage(Component.translatable(key), true);
            syncState(player, true);
            return result;
        }

        player.setData(ModAttachments.STATS, current.plus(delta));
        refreshStats(player);
        syncState(player, false);
        return BatchAllocateResult.OK;
    }

    public static void resetStats(ServerPlayer player) {
        player.setData(ModAttachments.STATS, WispStats.ZERO);
        refreshStats(player);
        syncState(player, false);
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
                level(player),
                WispbornConfig.MANA_PER_LEVEL.get(),
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
        syncState(player, false);
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

    public static SyncWispStatePayload computeState(ServerPlayer player) {
        List<Integer> thresholds = WispbornConfig.levelThresholds();
        int totalEp = player.getData(ModAttachments.PROGRESS).totalEp();
        int currentLevel = LevelCurve.levelFor(totalEp, thresholds);
        int levelStartEp = LevelCurve.levelStartEp(currentLevel, thresholds);
        int nextLevelEp = LevelCurve.nextThreshold(totalEp, thresholds).orElse(-1);
        int mana = Math.round(getMana(player));
        int maxMana = getMaxMana(player);
        int availablePts = availablePoints(player);
        WispStats stats = player.getData(ModAttachments.STATS);
        int maxPerStat = WispbornConfig.MAX_PER_STAT.get();

        boolean hostConsumeReady = false;
        float hostEpFactor = 1.0F;
        if (HostManager.isHost(player)) {
            Optional<LivingEntity> hostOpt = HostManager.hostOf(player);
            if (hostOpt.isPresent() && hostOpt.get() instanceof Mob mob && mob.isAlive()) {
                hostConsumeReady = DrainMath.atFloor(
                        mob.getHealth(),
                        mob.getMaxHealth(),
                        WispbornConfig.DRAIN_FLOOR_PERCENT.getAsDouble()
                );
                int mobLevel = MobTraitsManager.traitsOf(mob).map(MobTraits::level).orElse(1);
                hostEpFactor = (float) EpMath.levelFactor(
                        currentLevel,
                        mobLevel,
                        WispbornConfig.EP_LEVEL_GRACE.getAsInt(),
                        WispbornConfig.EP_FALLOFF_PER_LEVEL.getAsDouble()
                );
            }
        }

        return new SyncWispStatePayload(
                currentLevel,
                totalEp,
                levelStartEp,
                nextLevelEp,
                mana,
                maxMana,
                availablePts,
                stats,
                maxPerStat,
                hostConsumeReady,
                hostEpFactor
        );
    }

    public static void syncState(ServerPlayer player, boolean force) {
        SyncWispStatePayload next = computeState(player);
        SyncWispStatePayload previous = LAST_SYNCED_STATE.get(player.getUUID());
        if (force || !next.equals(previous)) {
            LAST_SYNCED_STATE.put(player.getUUID(), next);
            PacketDistributor.sendToPlayer(player, next);
        }
    }

    public static void clearSyncCache(ServerPlayer player) {
        LAST_SYNCED_STATE.remove(player.getUUID());
    }
}
