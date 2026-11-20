package dev.eder.wispborn.event;

import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormType;
import dev.eder.wispborn.host.HostManager;
import dev.eder.wispborn.host.StealthRules;
import dev.eder.wispborn.registry.ModAttachments;
import dev.eder.wispborn.registry.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
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

        boolean consumed = event.getSource().is(ModDamageTypes.CONSUMED);
        List<Entity> passengers = new ArrayList<>(dying.getPassengers());
        for (Entity passenger : passengers) {
            if (passenger instanceof ServerPlayer serverPlayer && HostManager.isHost(serverPlayer)) {
                if (consumed && dying instanceof Mob mob) {
                    HostManager.onHostConsumed(serverPlayer, mob);
                    HostManager.release(serverPlayer, HostManager.Reason.CONSUMED);
                } else {
                    HostManager.release(serverPlayer, HostManager.Reason.HOST_DIED);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().is(ModDamageTypes.CONSUMED)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        DamageSource lastSource = event.getEntity().getLastDamageSource();
        if (lastSource != null && lastSource.is(ModDamageTypes.CONSUMED)) {
            event.setDroppedExperience(0);
            event.setCanceled(true);
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
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        boolean targetIsPlayer = newTarget instanceof Player;
        FormType targetForm = newTarget instanceof Player player
                ? player.getData(ModAttachments.FORM).type()
                : FormType.WISP;
        if (StealthRules.blocksTargeting(targetIsPlayer, targetForm)) {
            if (event.getEntity() instanceof Mob mob && mob.getTarget() == newTarget) {
                event.setNewAboutToBeSetTarget(null);
            } else {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HostManager.tick(serverPlayer);
            HostManager.tickDrain(serverPlayer);
        }
    }
}
