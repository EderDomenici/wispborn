package dev.eder.wispborn.host;

import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.form.FormType;
import dev.eder.wispborn.form.WispAnimatable;
import dev.eder.wispborn.progress.ProgressManager;
import dev.eder.wispborn.progress.TierResolver;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HostManager {
    public static final int POSSESS_DELAY_TICKS = 16;
    public static final double MAX_INFEST_DISTANCE_SQR = 4.0D * 4.0D;

    public enum Reason {
        SNEAK,
        HOST_DIED,
        LOGOUT,
        CONSUMED
    }

    private record PendingInfest(int targetEntityId, long executeAtGameTime) {
    }

    private static final Map<UUID, PendingInfest> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> PENDING_DISMOUNT_POS = new ConcurrentHashMap<>();

    private HostManager() {
    }

    public static boolean isHost(Player player) {
        return player.getData(ModAttachments.FORM).type() == FormType.HOST;
    }

    public static Optional<LivingEntity> hostOf(Player player) {
        if (!isHost(player)) {
            return Optional.empty();
        }
        Entity vehicle = player.getVehicle();
        return vehicle instanceof LivingEntity living ? Optional.of(living) : Optional.empty();
    }

    public static InfestResult tryInfest(ServerPlayer player, Mob target) {
        OptionalInt tierOpt = TierResolver.tierOf(target.getType());
        int playerLevel = ProgressManager.level(player);
        List<Integer> minLevels = WispbornConfig.tiersMinLevel();
        int maxTier = TierResolver.maxTierFor(playerLevel, minLevels);
        float mana = ProgressManager.getMana(player);
        int costPerTier = WispbornConfig.MANA_INFEST_COST_PER_TIER.get();

        InfestResult result = InfestRules.check(
                true,
                target.isAlive(),
                !target.getPassengers().isEmpty(),
                tierOpt,
                maxTier,
                mana,
                costPerTier
        );

        if (result != InfestResult.OK) {
            sendInfestFailure(player, result, tierOpt, minLevels, mana, costPerTier);
            return result;
        }

        PendingInfest currentPending = PENDING.get(player.getUUID());
        long now = player.serverLevel().getGameTime();
        if (currentPending != null
                && currentPending.targetEntityId() == target.getId()
                && now < currentPending.executeAtGameTime()) {
            return InfestResult.OK;
        }

        PENDING.put(player.getUUID(), new PendingInfest(target.getId(), now + POSSESS_DELAY_TICKS));
        WispAnimatable.playPossess(player);
        return InfestResult.OK;
    }

    public static void tick(ServerPlayer player) {
        Vec3 dismountPos = PENDING_DISMOUNT_POS.remove(player.getUUID());
        if (dismountPos != null) {
            player.teleportTo(dismountPos.x, dismountPos.y, dismountPos.z);
        }

        if (isHost(player)) {
            if (player.getRemainingFireTicks() > 0) {
                player.clearFire();
            }
            if (player.tickCount % 20 == 0) {
                hostOf(player).ifPresent(host -> player.displayClientMessage(buildHostHud(host), true));
            }
            return;
        }

        PendingInfest pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return;
        }

        if (!FormManager.isWisp(player)) {
            PENDING.remove(player.getUUID());
            return;
        }

        ServerLevel serverLevel = player.serverLevel();
        if (serverLevel.getGameTime() < pending.executeAtGameTime()) {
            return;
        }

        PENDING.remove(player.getUUID());

        Entity targetEntity = serverLevel.getEntity(pending.targetEntityId());
        if (!(targetEntity instanceof Mob mob) || player.distanceToSqr(mob) > MAX_INFEST_DISTANCE_SQR) {
            sendFailureMessageAndSound(player, Component.translatable("wispborn.infest.cancelled"));
            return;
        }

        OptionalInt tierOpt = TierResolver.tierOf(mob.getType());
        int playerLevel = ProgressManager.level(player);
        List<Integer> minLevels = WispbornConfig.tiersMinLevel();
        int maxTier = TierResolver.maxTierFor(playerLevel, minLevels);
        float mana = ProgressManager.getMana(player);
        int costPerTier = WispbornConfig.MANA_INFEST_COST_PER_TIER.get();

        InfestResult recheck = InfestRules.check(
                true,
                mob.isAlive(),
                !mob.getPassengers().isEmpty(),
                tierOpt,
                maxTier,
                mana,
                costPerTier
        );

        if (recheck != InfestResult.OK) {
            sendInfestFailure(player, recheck, tierOpt, minLevels, mana, costPerTier);
            return;
        }

        int tier = tierOpt.getAsInt();
        float cost = (float) costPerTier * tier;
        if (!ProgressManager.spendMana(player, cost)) {
            sendInfestFailure(player, InfestResult.NO_MANA, tierOpt, minLevels, ProgressManager.getMana(player), costPerTier);
            return;
        }

        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        FormManager.setForm(player, FormState.host(entityTypeId));
        player.setInvisible(true);
        player.startRiding(mob, true);

        serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.ALLAY_ITEM_GIVEN,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5D,
                mob.getZ(),
                20,
                mob.getBbWidth() * 0.4D,
                mob.getBbHeight() * 0.4D,
                mob.getBbWidth() * 0.4D,
                0.05D
        );
        player.displayClientMessage(Component.translatable("wispborn.infest.ok", mob.getType().getDescription()), true);
    }

    public static void release(ServerPlayer player, Reason reason) {
        PENDING.remove(player.getUUID());
        if (!isHost(player)) {
            return;
        }

        Entity vehicle = player.getVehicle();
        Vec3 exitPos = vehicle != null
                ? new Vec3(vehicle.getX(), vehicle.getY() + vehicle.getBbHeight() + 0.1D, vehicle.getZ())
                : null;

        FormManager.setForm(player, FormState.WISP);
        player.setInvisible(false);

        if (vehicle != null) {
            if (reason == Reason.SNEAK) {
                PENDING_DISMOUNT_POS.put(player.getUUID(), exitPos);
            } else {
                player.stopRiding();
                player.teleportTo(exitPos.x, exitPos.y, exitPos.z);
            }
        }

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ALLAY_ITEM_TAKEN,
                SoundSource.PLAYERS,
                1.0F,
                0.9F
        );

        if (reason == Reason.HOST_DIED) {
            player.displayClientMessage(Component.translatable("wispborn.host.died"), true);
        } else if (reason == Reason.SNEAK) {
            player.displayClientMessage(Component.translatable("wispborn.host.left"), true);
        }
    }

    public static void ensureValidHostState(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        PENDING_DISMOUNT_POS.remove(player.getUUID());
        if (isHost(player) && player.getVehicle() == null) {
            FormManager.setForm(player, FormState.WISP);
            player.setInvisible(false);
        }
    }

    public static void clearPending(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        PENDING_DISMOUNT_POS.remove(player.getUUID());
    }

    public static Component buildHostHud(LivingEntity host) {
        float maxHealth = Math.max(1.0F, host.getMaxHealth());
        int healthPercent = Math.round((host.getHealth() / maxHealth) * 100.0F);
        return Component.translatable("wispborn.hud.host", host.getType().getDescription(), healthPercent);
    }

    private static void sendInfestFailure(
            ServerPlayer player,
            InfestResult result,
            OptionalInt tierOpt,
            List<Integer> minLevels,
            float mana,
            int costPerTier
    ) {
        Component message = switch (result) {
            case IMMUNE, NOT_A_MOB -> Component.translatable("wispborn.infest.immune");
            case HAS_PASSENGER -> Component.translatable("wispborn.infest.has_passenger");
            case TOO_STRONG -> {
                int tier = tierOpt.orElse(1);
                int requiredLevel = (tier - 1 >= 0 && tier - 1 < minLevels.size()) ? minLevels.get(tier - 1) : tier;
                yield Component.translatable("wispborn.infest.too_strong", tier, requiredLevel);
            }
            case NO_MANA -> {
                int tier = tierOpt.orElse(1);
                int requiredMana = costPerTier * tier;
                yield Component.translatable("wispborn.infest.no_mana", Math.round(mana), requiredMana);
            }
            case OK -> Component.empty();
        };
        sendFailureMessageAndSound(player, message);
    }

    private static void sendFailureMessageAndSound(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS,
                0.7F,
                1.2F
        );
    }
}
