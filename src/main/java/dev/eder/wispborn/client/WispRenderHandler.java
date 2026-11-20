package dev.eder.wispborn.client;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.form.FormType;
import dev.eder.wispborn.form.WispTuning;
import dev.eder.wispborn.host.InfestResult;
import dev.eder.wispborn.network.TargetInfoPayload;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
    private static boolean renderingGrimoirePortrait = false;

    private WispRenderHandler() {
    }

    public static void setRenderingGrimoirePortrait(boolean rendering) {
        renderingGrimoirePortrait = rendering;
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        renderer = new WispPlayerRenderer(event.getContext());
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        FormState state = player.getData(ModAttachments.FORM);
        if (state.type() == FormType.HOST && !renderingGrimoirePortrait) {
            event.setCanceled(true);
            return;
        }
        if (state.type() != FormType.WISP && !(state.type() == FormType.HOST && renderingGrimoirePortrait)) {
            return;
        }

        event.setCanceled(true);

        if (renderer == null || (player.isInvisible() && !renderingGrimoirePortrait) || !(player instanceof AbstractClientPlayer clientPlayer)) {
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
        Player localPlayer = mc.player;
        if (level == null || localPlayer == null || mc.isPaused() || !FormManager.isWisp(localPlayer)) {
            return;
        }

        if (localPlayer.tickCount % 4 != 0) {
            return;
        }

        TargetInfoPayload targetInfo = ClientPossession.targetInfo();
        if (targetInfo.entityId() == -1) {
            return;
        }

        Entity target = level.getEntity(targetInfo.entityId());
        if (target == null || !target.isAlive()) {
            return;
        }

        int color = targetInfo.result() == InfestResult.OK
                ? WispTuning.TARGET_OK_COLOR
                : WispTuning.TARGET_FAIL_COLOR;
        DustParticleOptions dust = new DustParticleOptions(Vec3.fromRGB24(color).toVector3f(), 1.0F);
        double radius = Math.max(0.4D, target.getBbWidth() * 0.65D);
        double baseY = target.getY() + 0.08D;

        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2.0D * i) / 6.0D;
            double px = target.getX() + Math.cos(angle) * radius;
            double pz = target.getZ() + Math.sin(angle) * radius;
            level.addParticle(dust, px, baseY, pz, 0.0D, 0.0D, 0.0D);
        }
    }
}
