package dev.eder.wispborn.event;

import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.host.HostManager;
import dev.eder.wispborn.network.SyncFormPayload;
import dev.eder.wispborn.progress.ProgressManager;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
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
        if (serverPlayer.tickCount % 20 != 0) {
            return;
        }
        // Cobre login e recarga do config: se a curva nova baixou o nível, zera os atributos excedentes.
        ProgressManager.validateStats(serverPlayer);
        if (FormManager.isWisp(serverPlayer)) {
            float regen = (float) WispbornConfig.MANA_REGEN_PER_SECOND.getAsDouble();
            ProgressManager.addMana(serverPlayer, regen);
            serverPlayer.displayClientMessage(ProgressManager.buildWispHud(serverPlayer), true);
        }
    }
}
