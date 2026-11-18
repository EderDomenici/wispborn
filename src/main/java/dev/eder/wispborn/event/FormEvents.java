package dev.eder.wispborn.event;

import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.form.HungerMath;
import dev.eder.wispborn.host.HostManager;
import dev.eder.wispborn.network.SyncFormPayload;
import dev.eder.wispborn.progress.ProgressManager;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FormEvents {
    private FormEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HostManager.ensureValidHostState(serverPlayer);
            FormManager.applyModifiers(serverPlayer);
            FormManager.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HostManager.ensureValidHostState(serverPlayer);
            FormManager.applyModifiers(serverPlayer);
            FormManager.sync(serverPlayer);
            ProgressManager.setMana(serverPlayer, ProgressManager.getMaxMana(serverPlayer));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            FormManager.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer tracker) {
            FormState state = target.getData(ModAttachments.FORM);
            PacketDistributor.sendToPlayer(tracker, new SyncFormPayload(target.getId(), state));
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        boolean isWisp = FormManager.isWisp(serverPlayer);
        Difficulty difficulty = serverPlayer.level().getDifficulty();

        if (serverPlayer.tickCount % 20 == 0) {
            // Cobre login e recarga do config: se a curva nova baixou o nível, zera os atributos excedentes.
            ProgressManager.validateStats(serverPlayer);
            if (isWisp) {
                float regen = (float) WispbornConfig.MANA_REGEN_PER_SECOND.getAsDouble();
                ProgressManager.addMana(serverPlayer, regen);
                if (difficulty != Difficulty.PEACEFUL) {
                    float exhaustion = HungerMath.exhaustionPerSecond(
                            ProgressManager.level(serverPlayer),
                            WispbornConfig.HUNGER_EXHAUSTION_BASE_PER_SECOND.getAsDouble(),
                            WispbornConfig.HUNGER_EXHAUSTION_PER_LEVEL_PER_SECOND.getAsDouble()
                    );
                    serverPlayer.causeFoodExhaustion(exhaustion);
                }
                serverPlayer.displayClientMessage(ProgressManager.buildWispHud(serverPlayer), true);
            }
        }

        int starveInterval = WispbornConfig.HUNGER_STARVE_INTERVAL_TICKS.getAsInt();
        if (serverPlayer.tickCount % starveInterval == 0 && serverPlayer.isAlive()) {
            boolean shouldStarve = HungerMath.wispStarves(
                    isWisp,
                    difficulty,
                    serverPlayer.getFoodData().getFoodLevel(),
                    serverPlayer.getHealth()
            );
            if (shouldStarve) {
                float damage = (float) WispbornConfig.HUNGER_STARVE_DAMAGE.getAsDouble();
                if (damage > 0.0F) {
                    serverPlayer.hurt(serverPlayer.damageSources().starve(), damage);
                }
            }
        }
    }
}
