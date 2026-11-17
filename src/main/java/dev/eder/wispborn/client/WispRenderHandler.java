package dev.eder.wispborn.client;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.form.FormType;
import dev.eder.wispborn.form.WispTuning;
import dev.eder.wispborn.host.InfestResult;
import dev.eder.wispborn.network.CastStatePayload;
import dev.eder.wispborn.network.TargetInfoPayload;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class WispRenderHandler {
    private static final ResourceLocation POSSESS_TARGET_LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "possess_target");

    private static WispPlayerRenderer renderer;

    private WispRenderHandler() {
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        renderer = new WispPlayerRenderer(event.getContext());
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(POSSESS_TARGET_LAYER_ID, WispRenderHandler::renderPossessTargetHud);
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

    private static void renderPossessTargetHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || !FormManager.isWisp(mc.player)) {
            return;
        }

        TargetInfoPayload info = ClientPossession.targetInfo();
        if (info.entityId() == -1) {
            return;
        }

        Entity target = mc.level.getEntity(info.entityId());
        if (target == null || !target.isAlive()) {
            return;
        }

        Font font = mc.font;
        int centerX = graphics.guiWidth() / 2;
        int baseY = graphics.guiHeight() / 2 + 12;

        Component header = info.tier() > 0
                ? Component.translatable("wispborn.target.info", target.getType().getDescription(), info.tier(), info.manaCost())
                : target.getType().getDescription();
        graphics.drawCenteredString(font, header, centerX, baseY, 0xFFFFFF);

        Component status = buildStatusLine(info);
        int statusColor = info.result() == InfestResult.OK ? 0x55FF55 : 0xFF5555;
        graphics.drawCenteredString(font, status, centerX, baseY + 11, statusColor);

        CastStatePayload cast = ClientPossession.castState();
        if (cast.targetEntityId() == info.entityId() && cast.durationTicks() > 0) {
            float elapsed = (mc.level.getGameTime() - ClientPossession.castStartGameTime())
                    + deltaTracker.getGameTimeDeltaPartialTick(false);
            float progress = Mth.clamp(elapsed / (float) cast.durationTicks(), 0.0F, 1.0F);

            int barWidth = 64;
            int barHeight = 5;
            int barX = centerX - (barWidth / 2);
            int barY = baseY + 23;

            graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xCC000000);
            int fillWidth = Math.round(barWidth * progress);
            if (fillWidth > 0) {
                graphics.fill(barX, barY, barX + fillWidth, barY + barHeight, WispTuning.POSSESSED_PARTICLE_COLOR);
            }
        }
    }

    private static Component buildStatusLine(TargetInfoPayload info) {
        return switch (info.result()) {
            case OK -> Component.translatable("wispborn.target.ready", KeyBindings.POSSESS.getTranslatedKeyMessage());
            case IMMUNE, NOT_A_MOB -> Component.translatable("wispborn.infest.immune");
            case HAS_PASSENGER -> Component.translatable("wispborn.infest.has_passenger");
            case TOO_STRONG -> Component.translatable("wispborn.infest.too_strong", info.tier(), Math.max(1, info.tier() * 2 - 1));
            case NO_MANA -> Component.translatable("wispborn.infest.no_mana", info.manaCost());
        };
    }
}
