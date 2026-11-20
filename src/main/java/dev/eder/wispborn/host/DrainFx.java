package dev.eder.wispborn.host;

import dev.eder.wispborn.form.WispTuning;
import dev.eder.wispborn.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feedback visual e sonoro da drenagem (design de 20/11/2026). Só efeito: nada aqui causa dano nem
 * informa a IA do mob, para o hospedeiro não fugir nem reagir.
 */
public final class DrainFx {
    /** Ticks desde o último pulso, por jogador, enquanto a onda do pulso está expandindo. */
    private static final Map<UUID, Integer> WAVE_TICKS = new ConcurrentHashMap<>();

    private static final DustParticleOptions WAVE_DUST = new DustParticleOptions(
            colorOf(WispTuning.POSSESSED_PARTICLE_COLOR), WispTuning.DRAIN_WAVE_PARTICLE_SCALE);

    private DrainFx() {
    }

    /** Chamado no pulso, depois do {@code setHealth}. */
    public static void onPulse(ServerPlayer player, Mob mob, float healthAfter, double floorPercent) {
        ServerLevel level = player.serverLevel();
        level.getChunkSource().broadcast(mob, new ClientboundHurtAnimationPacket(mob));
        level.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                ModSounds.HOST_DRAIN_PULSE.get(),
                SoundSource.NEUTRAL,
                WispTuning.DRAIN_PULSE_VOLUME,
                DrainMath.pulsePitch(healthAfter, mob.getMaxHealth(), floorPercent)
        );
        WAVE_TICKS.put(player.getUUID(), 0);
    }

    /** Chamado todo tick em HOST: avança a onda do último pulso ou, no piso, desenha o anel girando. */
    public static void tick(ServerPlayer player, Mob mob, boolean atFloor) {
        UUID id = player.getUUID();
        Integer waveTick = WAVE_TICKS.get(id);
        if (waveTick != null) {
            int interval = WispTuning.DRAIN_WAVE_STEP_TICKS;
            int steps = WispTuning.DRAIN_WAVE_STEPS;
            if (waveTick % interval == 0) {
                float start = mob.getBbWidth() * 0.6F;
                float end = mob.getBbWidth() + 1.0F;
                spawnRing(player.serverLevel(), mob, DrainMath.ringRadius(waveTick / interval, steps, start, end), 0.0D);
            }
            if (waveTick >= interval * steps) {
                WAVE_TICKS.remove(id);
            } else {
                WAVE_TICKS.put(id, waveTick + 1);
            }
            return;
        }

        if (atFloor && player.tickCount % WispTuning.DRAIN_FLOOR_RING_INTERVAL_TICKS == 0) {
            double angleOffset = player.tickCount * WispTuning.DRAIN_FLOOR_RING_SPIN_PER_TICK;
            spawnRing(player.serverLevel(), mob, mob.getBbWidth() * 0.6F + 0.4F, angleOffset);
        }
    }

    public static void clear(UUID playerId) {
        WAVE_TICKS.remove(playerId);
    }

    private static void spawnRing(ServerLevel level, Mob mob, float radius, double angleOffset) {
        int points = DrainMath.ringPoints(radius);
        double y = mob.getY() + mob.getBbHeight() * 0.5D;
        for (int i = 0; i < points; i++) {
            double angle = angleOffset + (Math.PI * 2.0D * i) / points;
            level.sendParticles(
                    WAVE_DUST,
                    mob.getX() + Math.cos(angle) * radius,
                    y,
                    mob.getZ() + Math.sin(angle) * radius,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private static Vector3f colorOf(int argb) {
        return new Vector3f(
                ((argb >> 16) & 0xFF) / 255.0F,
                ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F
        );
    }
}
