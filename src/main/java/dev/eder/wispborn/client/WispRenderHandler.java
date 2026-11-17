package dev.eder.wispborn.client;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.form.FormType;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class WispRenderHandler {
    private static WispPlayerRenderer renderer;

    private WispRenderHandler() {
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        renderer = new WispPlayerRenderer(event.getContext());
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        FormState state = player.getData(ModAttachments.FORM);
        if (state.type() == FormType.HOST) {
            event.setCanceled(true);
            return;
        }
        if (state.type() != FormType.WISP) {
            return;
        }

        event.setCanceled(true);

        if (renderer == null || player.isInvisible() || !(player instanceof AbstractClientPlayer clientPlayer)) {
            return;
        }

        float partialTick = event.getPartialTick();
        float yaw = Mth.lerp(partialTick, clientPlayer.yRotO, clientPlayer.getYRot());
        renderer.render(
                clientPlayer,
                yaw,
                partialTick,
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight()
        );
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null && FormManager.isParasite(localPlayer)) {
            event.setCanceled(true);
        }
    }
}
