package dev.eder.wispborn.client;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Enquanto parasita (WISP ou HOST), os botões do mouse não fazem nada, nem animação. As travas de
 * {@code WispRestrictions} no servidor bloqueiam o resultado, mas sem isto o cliente prevê a ação:
 * balança o braço, racha o bloco e o apaga até o servidor corrigir.
 */
@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class ParasiteInputBlocker {
    private ParasiteInputBlocker() {
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && FormManager.isParasite(player)) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
