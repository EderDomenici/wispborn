package dev.eder.wispborn.event;

import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.WispSoundRules;
import dev.eder.wispborn.form.WispTuning;
import dev.eder.wispborn.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WispSounds {
    private static final double SAME_POSITION_SQR = 1.0E-4;
    private static final Map<UUID, Long> CLIENT_LAST_DRIFT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SERVER_LAST_DRIFT = new ConcurrentHashMap<>();

    private WispSounds() {
    }

    @SubscribeEvent
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getEntity() instanceof Player player) || !FormManager.isWisp(player)) {
            return;
        }
        handleWispSound(event, player);
    }

    @SubscribeEvent
    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        Holder<SoundEvent> sound = event.getSound();
        if (sound == null) {
            return;
        }
        WispSoundRules.SoundKind kind = WispSoundRules.classify(sound.value().getLocation().getPath());
        if (kind == WispSoundRules.SoundKind.OTHER) {
            return;
        }

        Vec3 position = event.getPosition();
        for (Player player : event.getLevel().players()) {
            if (FormManager.isWisp(player) && player.position().distanceToSqr(position) < SAME_POSITION_SQR) {
                applySoundRule(event, player, kind);
                return;
            }
        }
    }

    private static void handleWispSound(PlayLevelSoundEvent event, Player player) {
        Holder<SoundEvent> sound = event.getSound();
        if (sound == null) {
            return;
        }
        WispSoundRules.SoundKind kind = WispSoundRules.classify(sound.value().getLocation().getPath());
        if (kind != WispSoundRules.SoundKind.OTHER) {
            applySoundRule(event, player, kind);
        }
    }

    private static void applySoundRule(PlayLevelSoundEvent event, Player player, WispSoundRules.SoundKind kind) {
        if (kind == WispSoundRules.SoundKind.FALL) {
            event.setCanceled(true);
            return;
        }

        Map<UUID, Long> tracker = player.level().isClientSide() ? CLIENT_LAST_DRIFT : SERVER_LAST_DRIFT;
        long now = player.level().getGameTime();
        long lastTick = tracker.getOrDefault(player.getUUID(), -1L);

        if (WispSoundRules.shouldPlayDrift(lastTick, now, WispTuning.DRIFT_SOUND_INTERVAL_TICKS)) {
            tracker.put(player.getUUID(), now);
            event.setSound(ModSounds.WISP_DRIFT);
        } else {
            event.setCanceled(true);
        }
    }
}
