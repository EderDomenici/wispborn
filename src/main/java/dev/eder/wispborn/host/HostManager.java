package dev.eder.wispborn.host;

import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.form.FormType;
import dev.eder.wispborn.form.HungerMath;
import dev.eder.wispborn.form.WispAnimatable;
import dev.eder.wispborn.form.WispTuning;
import dev.eder.wispborn.network.CastStatePayload;
import dev.eder.wispborn.network.TargetInfoPayload;
import dev.eder.wispborn.progress.EpMath;
import dev.eder.wispborn.progress.ProgressManager;
import dev.eder.wispborn.progress.StatMath;
import dev.eder.wispborn.progress.WispStats;
import dev.eder.wispborn.registry.ModAttachments;
import dev.eder.wispborn.registry.ModDamageTypes;
import dev.eder.wispborn.traits.MobTraits;
import dev.eder.wispborn.traits.MobTraitsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HostManager {

    public enum Reason {
        SNEAK,
        HOST_DIED,
        LOGOUT,
        CONSUMED
    }

    private record PendingInfest(int targetEntityId, long executeAtGameTime) {
    }

    private static final Map<UUID, PendingInfest> PENDING = new ConcurrentHashMap<>();
    private static final Set<UUID> DAMAGED_DURING_CAST = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Vec3> PENDING_DISMOUNT_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, TargetInfoPayload> LAST_TARGET_INFO = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DRAIN_TICKS = new ConcurrentHashMap<>();

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

    public static void handlePossessStart(ServerPlayer player, int targetEntityId) {
        if (!FormManager.isWisp(player)) {
            return;
        }
        Entity entity = player.serverLevel().getEntity(targetEntityId);
        if (!(entity instanceof Mob target)) {
            sendFailureMessageAndSound(player, Component.translatable("wispborn.infest.immune"));
            return;
        }
        tryInfest(player, target);
    }

    public static InfestResult tryInfest(ServerPlayer player, Mob target) {
        double range = WispbornConfig.POSSESS_RANGE.getAsDouble();
        double rangeSqr = range * range;
        if (player.distanceToSqr(target) > rangeSqr || !player.hasLineOfSight(target)) {
            sendFailureMessageAndSound(player, Component.translatable("wispborn.infest.cancelled"));
            return InfestResult.NOT_A_MOB;
        }

        Optional<MobTraits> traitsOpt = MobTraitsManager.traitsOf(target);
        int playerLevel = ProgressManager.level(player);
        int gateMargin = WispbornConfig.INFEST_LEVEL_GATE_MARGIN.get();
        float mana = ProgressManager.getMana(player);
        int costPerLevel = WispbornConfig.MANA_INFEST_COST_PER_LEVEL.get();

        InfestResult result = InfestRules.check(
                true,
                target.isAlive(),
                !target.getPassengers().isEmpty(),
                traitsOpt,
                playerLevel,
                gateMargin,
                mana,
                costPerLevel
        );

        if (result != InfestResult.OK) {
            sendInfestFailure(player, result, traitsOpt, playerLevel, mana, costPerLevel);
            return result;
        }

        int castTicks = WispbornConfig.POSSESS_CAST_TICKS.get();
        long now = player.serverLevel().getGameTime();
        PendingInfest currentPending = PENDING.get(player.getUUID());
        if (currentPending != null
                && currentPending.targetEntityId() == target.getId()
                && now < currentPending.executeAtGameTime()) {
            return InfestResult.OK;
        }

        DAMAGED_DURING_CAST.remove(player.getUUID());
        PENDING.put(player.getUUID(), new PendingInfest(target.getId(), now + castTicks));
        WispAnimatable.playPossess(player);
        PacketDistributor.sendToPlayer(player, new CastStatePayload(target.getId(), castTicks));
        return InfestResult.OK;
    }

    public static void notifyDamageTaken(ServerPlayer player) {
        if (PENDING.containsKey(player.getUUID())) {
            DAMAGED_DURING_CAST.add(player.getUUID());
        }
    }

    public static void cancelCast(ServerPlayer player, boolean sendCancelledMessage) {
        PendingInfest removed = PENDING.remove(player.getUUID());
        DAMAGED_DURING_CAST.remove(player.getUUID());
        if (removed == null) {
            return;
        }
        WispAnimatable.stopPossess(player);
        PacketDistributor.sendToPlayer(player, CastStatePayload.NONE);
        if (sendCancelledMessage) {
            sendFailureMessageAndSound(player, Component.translatable("wispborn.infest.cancelled"));
        }
    }

    public static void tick(ServerPlayer player) {
        Vec3 dismountPos = PENDING_DISMOUNT_POS.remove(player.getUUID());
        if (dismountPos != null) {
            player.teleportTo(dismountPos.x, dismountPos.y, dismountPos.z);
        }

        if (isHost(player)) {
            syncTargetInfo(player, TargetInfoPayload.NONE);
            if (player.getRemainingFireTicks() > 0) {
                player.clearFire();
            }
            if (player.tickCount % 10 == 0) {
                hostOf(player).ifPresent(host -> spawnPossessedParticles(player.serverLevel(), host));
            }
            if (player.tickCount % 20 == 0) {
                HostStealth.clearPursuers(player);
                hostOf(player).ifPresent(host -> player.displayClientMessage(buildHostHud(player, host), true));
            }
            return;
        }

        if (!FormManager.isWisp(player)) {
            if (PENDING.containsKey(player.getUUID())) {
                cancelCast(player, true);
            }
            syncTargetInfo(player, TargetInfoPayload.NONE);
            return;
        }

        if (player.tickCount % 4 == 0) {
            syncTargetInfo(player, computeTargetInfo(player));
        }

        PendingInfest pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return;
        }

        ServerLevel serverLevel = player.serverLevel();
        Entity targetEntity = serverLevel.getEntity(pending.targetEntityId());
        Mob mob = (targetEntity instanceof Mob m) ? m : null;

        double range = WispbornConfig.POSSESS_RANGE.getAsDouble();
        double rangeSqr = range * range;
        boolean targetAlive = mob != null && mob.isAlive();
        double distSqr = mob != null ? player.distanceToSqr(mob) : Double.MAX_VALUE;
        boolean hasLos = mob != null && player.hasLineOfSight(mob);
        boolean tookDamage = DAMAGED_DURING_CAST.remove(player.getUUID());

        Optional<CancelReason> cancel = CastRules.cancelReason(
                FormManager.isWisp(player),
                targetAlive,
                distSqr,
                rangeSqr,
                hasLos,
                tookDamage
        );

        if (cancel.isPresent()) {
            cancelCast(player, true);
            return;
        }

        if (serverLevel.getGameTime() < pending.executeAtGameTime()) {
            return;
        }

        PENDING.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, CastStatePayload.NONE);

        Optional<MobTraits> traitsOpt = MobTraitsManager.traitsOf(mob);
        int playerLevel = ProgressManager.level(player);
        int gateMargin = WispbornConfig.INFEST_LEVEL_GATE_MARGIN.get();
        float mana = ProgressManager.getMana(player);
        int costPerLevel = WispbornConfig.MANA_INFEST_COST_PER_LEVEL.get();

        InfestResult recheck = InfestRules.check(
                true,
                mob.isAlive(),
                !mob.getPassengers().isEmpty(),
                traitsOpt,
                playerLevel,
                gateMargin,
                mana,
                costPerLevel
        );

        if (recheck != InfestResult.OK) {
            sendInfestFailure(player, recheck, traitsOpt, playerLevel, mana, costPerLevel);
            return;
        }

        int mobLevel = traitsOpt.get().level();
        float cost = (float) costPerLevel * mobLevel;
        if (!ProgressManager.spendMana(player, cost)) {
            sendInfestFailure(player, InfestResult.NO_MANA, traitsOpt, playerLevel, ProgressManager.getMana(player), costPerLevel);
            return;
        }

        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        FormManager.setForm(player, FormState.host(entityTypeId));
        player.setInvisible(true);
        player.startRiding(mob, true);
        DRAIN_TICKS.put(player.getUUID(), -1);
        HostStealth.clearPursuers(player);
        syncTargetInfo(player, TargetInfoPayload.NONE);

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

    public static void tickDrain(ServerPlayer player) {
        if (!isHost(player)) {
            DRAIN_TICKS.remove(player.getUUID());
            DrainFx.clear(player.getUUID());
            return;
        }

        Optional<LivingEntity> hostOpt = hostOf(player);
        if (hostOpt.isEmpty() || !hostOpt.get().isAlive()) {
            return;
        }

        if (!(hostOpt.get() instanceof Mob mob)) {
            release(player, Reason.SNEAK);
            return;
        }

        Optional<MobTraits> traitsOpt = MobTraitsManager.traitsOf(mob);
        if (traitsOpt.isEmpty()) {
            release(player, Reason.SNEAK);
            return;
        }

        float health = mob.getHealth();
        float maxHealth = mob.getMaxHealth();
        double floorPercent = WispbornConfig.DRAIN_FLOOR_PERCENT.getAsDouble();
        boolean atFloor = DrainMath.atFloor(health, maxHealth, floorPercent);
        DrainFx.tick(player, mob, atFloor);

        int ticks = DRAIN_TICKS.merge(player.getUUID(), 1, Integer::sum);
        int interval = WispbornConfig.DRAIN_INTERVAL_TICKS.getAsInt();
        if (ticks <= 0 || interval <= 0 || ticks % interval != 0) {
            return;
        }

        if (atFloor) {
            return;
        }

        WispStats stats = player.getData(ModAttachments.STATS);
        double drainPct = StatMath.drainPercent(
                WispbornConfig.DRAIN_BASE_PERCENT_PER_PULSE.getAsDouble(),
                stats.absorption(),
                WispbornConfig.ABSORPTION_PER_POINT.getAsDouble()
        );
        float nextHealth = DrainMath.nextHealth(health, maxHealth, drainPct, floorPercent);
        mob.setHealth(nextHealth);

        int wispLevel = ProgressManager.level(player);
        int mobLevel = traitsOpt.get().level();
        double factor = EpMath.levelFactor(
                wispLevel,
                mobLevel,
                WispbornConfig.EP_LEVEL_GRACE.getAsInt(),
                WispbornConfig.EP_FALLOFF_PER_LEVEL.getAsDouble()
        );
        int baseEp = DrainMath.epPerPulse(
                mobLevel,
                WispbornConfig.EP_PER_PULSE_PER_LEVEL.getAsInt(),
                factor
        );
        if (baseEp > 0) {
            ProgressManager.addEp(player, baseEp);
        }

        int restoreFood = WispbornConfig.HUNGER_RESTORE_PER_PULSE.getAsInt();
        float restoreSaturation = (float) WispbornConfig.HUNGER_RESTORE_PER_PULSE_SATURATION.getAsDouble();
        if (restoreFood > 0 || restoreSaturation > 0.0F) {
            player.getFoodData().eat(restoreFood, restoreSaturation);
        }

        DrainFx.onPulse(player, mob, nextHealth, floorPercent);
        player.displayClientMessage(buildHostHud(player, mob), true);
    }

    public static ConsumeResult tryConsume(ServerPlayer player) {
        boolean hostState = isHost(player);
        Optional<LivingEntity> hostOpt = hostOf(player);
        Mob mob = (hostOpt.orElse(null) instanceof Mob m) ? m : null;
        boolean hostAlive = mob != null && mob.isAlive();
        float health = mob != null ? mob.getHealth() : 0.0F;
        float maxHealth = mob != null ? mob.getMaxHealth() : 1.0F;
        double floorPercent = WispbornConfig.DRAIN_FLOOR_PERCENT.getAsDouble();

        ConsumeResult result = ConsumeRules.check(hostState, hostAlive, health, maxHealth, floorPercent);
        if (result == ConsumeResult.NOT_HOST) {
            return ConsumeResult.NOT_HOST;
        }
        if (result == ConsumeResult.NOT_READY) {
            int healthPercent = Math.round((health / Math.max(1.0F, maxHealth)) * 100.0F);
            player.displayClientMessage(Component.translatable("wispborn.consume.not_ready", healthPercent), true);
            return ConsumeResult.NOT_READY;
        }

        if (MobTraitsManager.traitsOf(mob).isEmpty()) {
            release(player, Reason.SNEAK);
            return ConsumeResult.NOT_HOST;
        }

        ServerLevel serverLevel = player.serverLevel();
        Holder<DamageType> consumedType = serverLevel.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageTypes.CONSUMED);
        DamageSource source = new DamageSource(consumedType, player);
        mob.hurt(source, Float.MAX_VALUE);
        return ConsumeResult.OK;
    }

    public static void onHostConsumed(ServerPlayer player, Mob mob) {
        int mobLevel = MobTraitsManager.traitsOf(mob).map(MobTraits::level).orElse(1);
        int wispLevel = ProgressManager.level(player);
        double factor = EpMath.levelFactor(
                wispLevel,
                mobLevel,
                WispbornConfig.EP_LEVEL_GRACE.getAsInt(),
                WispbornConfig.EP_FALLOFF_PER_LEVEL.getAsDouble()
        );
        int baseEp = ConsumeRules.consumeEp(
                mobLevel,
                WispbornConfig.EP_CONSUME_PER_LEVEL.getAsInt(),
                factor
        );
        int gainedEp = baseEp > 0 ? ProgressManager.addEp(player, baseEp) : 0;

        int foodRestore = HungerMath.consumeFood(mobLevel, WispbornConfig.HUNGER_CONSUME_BASE.getAsInt());
        float saturationRestore = (float) WispbornConfig.HUNGER_CONSUME_SATURATION.getAsDouble();
        if (foodRestore > 0 || saturationRestore > 0.0F) {
            player.getFoodData().eat(foodRestore, saturationRestore);
        }

        ServerLevel serverLevel = player.serverLevel();
        serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.GENERIC_EAT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        serverLevel.sendParticles(
                ParticleTypes.SOUL,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5D,
                mob.getZ(),
                20,
                mob.getBbWidth() * 0.4D,
                mob.getBbHeight() * 0.4D,
                mob.getBbWidth() * 0.4D,
                0.05D
        );

        Component mobName = mob.getType().getDescription();
        Component message = gainedEp > 0
                ? Component.translatable("wispborn.consume.ok", mobName, gainedEp)
                : Component.translatable("wispborn.consume.ok_no_ep", mobName);
        player.displayClientMessage(message, true);
        player.sendSystemMessage(message);
    }

    public static void release(ServerPlayer player, Reason reason) {
        cancelCast(player, false);
        LAST_TARGET_INFO.remove(player.getUUID());
        DRAIN_TICKS.remove(player.getUUID());
        DrainFx.clear(player.getUUID());
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
        clearPending(player);
        if (isHost(player) && player.getVehicle() == null) {
            FormManager.setForm(player, FormState.WISP);
            player.setInvisible(false);
        }
    }

    public static void clearPending(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        DAMAGED_DURING_CAST.remove(player.getUUID());
        PENDING_DISMOUNT_POS.remove(player.getUUID());
        LAST_TARGET_INFO.remove(player.getUUID());
        DRAIN_TICKS.remove(player.getUUID());
        DrainFx.clear(player.getUUID());
    }

    public static Component buildHostHud(ServerPlayer player, LivingEntity host) {
        float maxHealth = Math.max(1.0F, host.getMaxHealth());
        int healthPercent = Math.round((host.getHealth() / maxHealth) * 100.0F);
        int mobLevel = (host instanceof Mob mob)
                ? MobTraitsManager.traitsOf(mob).map(MobTraits::level).orElse(1)
                : 1;
        int wispLevel = ProgressManager.level(player);
        Component epDisplay = ProgressManager.epDisplay(player);
        int currentMana = Math.round(ProgressManager.getMana(player));
        int maxMana = ProgressManager.getMaxMana(player);

        MutableComponent hud = Component.translatable(
                "wispborn.hud.host",
                host.getType().getDescription(),
                mobLevel,
                healthPercent,
                epDisplay,
                wispLevel,
                currentMana,
                maxMana
        );

        double floorPercent = WispbornConfig.DRAIN_FLOOR_PERCENT.getAsDouble();
        if (DrainMath.atFloor(host.getHealth(), maxHealth, floorPercent)) {
            hud.append(Component.translatable("wispborn.hud.host.consume", Component.keybind("key.wispborn.consume")).withStyle(ChatFormatting.GREEN));
        }

        double factor = EpMath.levelFactor(
                wispLevel,
                mobLevel,
                WispbornConfig.EP_LEVEL_GRACE.getAsInt(),
                WispbornConfig.EP_FALLOFF_PER_LEVEL.getAsDouble()
        );
        if (factor <= 0.0D) {
            hud.append(Component.translatable("wispborn.hud.host.no_ep").withStyle(ChatFormatting.GRAY));
        }

        return hud;
    }

    private static void spawnPossessedParticles(ServerLevel serverLevel, LivingEntity host) {
        serverLevel.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, WispTuning.POSSESSED_PARTICLE_COLOR),
                host.getX(),
                host.getY() + host.getBbHeight() * 0.5D,
                host.getZ(),
                3,
                host.getBbWidth() * 0.35D,
                host.getBbHeight() * 0.35D,
                host.getBbWidth() * 0.35D,
                1.0D
        );
    }

    private static TargetInfoPayload computeTargetInfo(ServerPlayer player) {
        double range = WispbornConfig.POSSESS_RANGE.getAsDouble();
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        HitResult blockHit = player.pick(range, 1.0F, false);
        double maxDistSqr = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceToSqr(eyePos)
                : range * range;

        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                eyePos,
                endPos,
                searchBox,
                e -> !e.isSpectator() && (
                        (e instanceof Mob && e.isAlive())
                                || (e instanceof Player && e.isAlive())
                                || (e instanceof PartEntity<?> part && part.getParent() instanceof Mob parent && parent.isAlive())
                ),
                maxDistSqr
        );

        if (entityHit == null) {
            return TargetInfoPayload.NONE;
        }

        Entity hitEntity = entityHit.getEntity();
        Entity resolved = (hitEntity instanceof PartEntity<?> part) ? part.getParent() : hitEntity;
        int wispLevel = ProgressManager.level(player);

        if (resolved instanceof Player targetPlayer) {
            if (!targetPlayer.isAlive() || !player.hasLineOfSight(targetPlayer)) {
                return TargetInfoPayload.NONE;
            }
            return new TargetInfoPayload(targetPlayer.getId(), InfestResult.IMMUNE, 0, Optional.empty(), 0, wispLevel);
        }

        if (!(resolved instanceof Mob mob) || !mob.isAlive() || !player.hasLineOfSight(mob)) {
            return TargetInfoPayload.NONE;
        }

        Optional<MobTraits> traitsOpt = MobTraitsManager.traitsOf(mob);
        int costPerLevel = WispbornConfig.MANA_INFEST_COST_PER_LEVEL.get();
        int gateMargin = WispbornConfig.INFEST_LEVEL_GATE_MARGIN.get();

        InfestResult result = InfestRules.check(
                true,
                mob.isAlive(),
                !mob.getPassengers().isEmpty(),
                traitsOpt,
                wispLevel,
                gateMargin,
                ProgressManager.getMana(player),
                costPerLevel
        );

        if (traitsOpt.isEmpty()) {
            return new TargetInfoPayload(mob.getId(), result, 0, Optional.empty(), 0, wispLevel);
        }

        MobTraits traits = traitsOpt.get();
        int manaCost = traits.level() * costPerLevel;
        return new TargetInfoPayload(mob.getId(), result, traits.level(), Optional.of(traits.sex()), manaCost, wispLevel);
    }

    private static void syncTargetInfo(ServerPlayer player, TargetInfoPayload next) {
        TargetInfoPayload previous = LAST_TARGET_INFO.get(player.getUUID());
        if (!next.equals(previous)) {
            if (next.equals(TargetInfoPayload.NONE)) {
                LAST_TARGET_INFO.remove(player.getUUID());
            } else {
                LAST_TARGET_INFO.put(player.getUUID(), next);
            }
            PacketDistributor.sendToPlayer(player, next);
        }
    }

    private static void sendInfestFailure(
            ServerPlayer player,
            InfestResult result,
            Optional<MobTraits> traitsOpt,
            int wispLevel,
            float mana,
            int costPerLevel
    ) {
        Component message = switch (result) {
            case IMMUNE, NOT_A_MOB -> Component.translatable("wispborn.infest.immune");
            case HAS_PASSENGER -> Component.translatable("wispborn.infest.has_passenger");
            case TOO_STRONG -> {
                int mobLevel = traitsOpt.map(MobTraits::level).orElse(1);
                yield Component.translatable("wispborn.infest.too_strong", mobLevel, wispLevel);
            }
            case NO_MANA -> {
                int mobLevel = traitsOpt.map(MobTraits::level).orElse(1);
                int requiredMana = costPerLevel * mobLevel;
                yield Component.translatable("wispborn.infest.no_mana", Math.round(mana) + "/" + requiredMana);
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
