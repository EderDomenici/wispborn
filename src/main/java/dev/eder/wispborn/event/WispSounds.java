package dev.eder.wispborn.event;

import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.WispSoundRules;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/**
 * Silencia passos e quedas do jogador em forma de wisp.
 * O som de passo do jogador sai em {@code Level#playSound(player, x, y, z, ...)} na posição exata dele,
 * nos dois lados: o client toca o próprio passo e o servidor transmite para os outros jogadores.
 */
public final class WispSounds {
    private static final double SAME_POSITION_SQR = 1.0E-4;

    private WispSounds() {
    }

    @SubscribeEvent
    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        Holder<SoundEvent> sound = event.getSound();
        if (sound == null || !WispSoundRules.isFootstepSound(sound.value().getLocation().getPath())) {
            return;
        }

        Vec3 position = event.getPosition();
        for (Player player : event.getLevel().players()) {
            if (FormManager.isWisp(player) && player.position().distanceToSqr(position) < SAME_POSITION_SQR) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
