package dev.eder.wispborn.event;

import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.host.HostManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

public final class HostEvents {
    private HostEvents() {
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (event.isDismounting()
                && event.getEntityMounting() instanceof ServerPlayer serverPlayer
                && HostManager.isHost(serverPlayer)) {
            HostManager.release(serverPlayer, HostManager.Reason.SNEAK);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity dying = event.getEntity();
        if (dying.level().isClientSide() || dying.getPassengers().isEmpty()) {
            return;
        }

        List<Entity> passengers = new ArrayList<>(dying.getPassengers());
        for (Entity passenger : passengers) {
            if (passenger instanceof ServerPlayer serverPlayer && HostManager.isHost(serverPlayer)) {
                HostManager.release(serverPlayer, HostManager.Reason.HOST_DIED);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (HostManager.isHost(serverPlayer)) {
                HostManager.release(serverPlayer, HostManager.Reason.LOGOUT);
            } else {
                HostManager.clearPending(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (HostManager.isHost(serverPlayer)) {
            if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setCanceled(true);
            }
        } else if (FormManager.isWisp(serverPlayer)) {
            HostManager.notifyDamageTaken(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HostManager.tick(serverPlayer);
        }
    }
}
