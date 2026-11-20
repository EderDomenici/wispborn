package dev.eder.wispborn.host;

import dev.eder.wispborn.config.WispbornConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

import java.util.List;
import java.util.UUID;

public final class HostStealth {
    private HostStealth() {
    }

    /** Limpa alvo e raiva apontados para o jogador em todo mob no raio, inclusive o hospedeiro. */
    public static void clearPursuers(ServerPlayer player) {
        double radius = WispbornConfig.STEALTH_CLEAR_RADIUS.getAsDouble();
        List<Mob> nearbyMobs = player.serverLevel().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(radius)
        );
        UUID playerId = player.getUUID();

        for (Mob mob : nearbyMobs) {
            Brain<?> brain = mob.getBrain();
            boolean directTarget = mob.getTarget() == player;
            boolean brainTarget = brain.isMemoryValue(MemoryModuleType.ATTACK_TARGET, player);
            boolean brainAngry = brain.isMemoryValue(MemoryModuleType.ANGRY_AT, playerId);
            boolean neutralAngry = mob instanceof NeutralMob neutral
                    && playerId.equals(neutral.getPersistentAngerTarget());
            boolean wardenAngry = mob instanceof Warden warden
                    && warden.getAngerManagement().getActiveAnger(player) > 0;

            boolean targetsPlayer = directTarget || brainTarget;
            boolean angryAtPlayer = brainAngry || neutralAngry || wardenAngry;

            if (!StealthRules.shouldClear(targetsPlayer, angryAtPlayer)) {
                continue;
            }

            if (directTarget) {
                mob.setTarget(null);
            }
            if (mob.getLastHurtByMob() == player) {
                mob.setLastHurtByMob(null);
            }
            if (brainTarget) {
                brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
            if (brainAngry) {
                brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            }
            if (neutralAngry && mob instanceof NeutralMob neutral) {
                neutral.stopBeingAngry();
            }
            if (mob instanceof Warden warden) {
                warden.clearAnger(player);
            }
        }
    }
}
