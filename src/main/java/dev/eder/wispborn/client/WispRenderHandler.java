package dev.eder.wispborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.form.FormType;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class WispRenderHandler {
    private static final ResourceLocation WISP_CORE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "textures/entity/wisp_core.png");
    private static final ResourceLocation WISP_HALO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "textures/entity/wisp_halo.png");

    private static final float CORE_RADIUS = 0.15F;
    private static final float HALO_RADIUS = 0.35F;
    private static final int HALO_ALPHA = 128;

    private WispRenderHandler() {
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

        float partialTick = event.getPartialTick();
        float time = (player.tickCount + partialTick) * 0.12F;
        float bobOffset = Mth.sin(time) * 0.08F;
        float pulseScale = 1.0F + Mth.sin(time * 1.5F) * 0.08F;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.35F + bobOffset, 0.0F);
        poseStack.scale(pulseScale, pulseScale, pulseScale);
        poseStack.mulPose(dispatcher.cameraOrientation());

        PoseStack.Pose pose = poseStack.last();
        drawBillboardQuad(bufferSource.getBuffer(RenderType.entityTranslucentEmissive(WISP_HALO_TEXTURE)),
                pose, HALO_RADIUS, 0.0F, HALO_ALPHA);
        drawBillboardQuad(bufferSource.getBuffer(RenderType.entityTranslucentEmissive(WISP_CORE_TEXTURE)),
                pose, CORE_RADIUS, 0.005F, 255);

        poseStack.popPose();
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

    private static void drawBillboardQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                          float radius, float zOffset, int alpha) {
        addQuadVertex(consumer, pose, -radius, -radius, zOffset, 0.0F, 1.0F, alpha);
        addQuadVertex(consumer, pose, radius, -radius, zOffset, 1.0F, 1.0F, alpha);
        addQuadVertex(consumer, pose, radius, radius, zOffset, 1.0F, 0.0F, alpha);
        addQuadVertex(consumer, pose, -radius, radius, zOffset, 0.0F, 0.0F, alpha);
    }

    private static void addQuadVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                      float x, float y, float z, float u, float v, int alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
