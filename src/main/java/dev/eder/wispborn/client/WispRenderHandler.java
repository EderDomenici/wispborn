package dev.eder.wispborn.client;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.form.FormType;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }

        for (Player player : level.players()) {
            if (!FormManager.isWisp(player) || player.isInvisible() || (player.tickCount % 2) != 0) {
                continue;
            }

            double dx = player.getX() - player.xo;
            double dy = player.getY() - player.yo;
            double dz = player.getZ() - player.zo;
            double speedSq = dx * dx + dz * dz;

            if (speedSq > 0.0004D) {
                level.addParticle(
                        ParticleTypes.END_ROD,
                        player.getX() - dx * 1.5D,
                        player.getY() + 0.35D - dy * 0.5D,
                        player.getZ() - dz * 1.5D,
                        -dx * 0.08D,
                        0.005D,
                        -dz * 0.08D
                );
            } else {
                double offsetX = (level.random.nextDouble() - 0.5D) * 0.4D;
                double offsetZ = (level.random.nextDouble() - 0.5D) * 0.4D;
                level.addParticle(
                        ParticleTypes.END_ROD,
                        player.getX() + offsetX,
                        player.getY() + 0.25D,
                        player.getZ() + offsetZ,
                        0.0D,
                        0.02D,
                        0.0D
                );
            }
        }
    }
}
