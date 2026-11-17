package dev.eder.wispborn.event;

import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.network.SyncFormPayload;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FormEvents {
    private FormEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            FormManager.applyModifiers(serverPlayer);
            FormManager.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
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
}
