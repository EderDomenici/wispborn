package dev.eder.wispborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.config.WispbornClientConfig;
import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.progress.EpMath;
import dev.eder.wispborn.traits.MobTraits;
import dev.eder.wispborn.traits.MobTraitsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

import java.util.Optional;

@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class MobNameplateRenderer {
    private static final float NAMETAG_SCALE = 0.025F;
    private static final int SEE_THROUGH_BASE_COLOR = 0x20FFFFFF;
    private static final int NORMAL_BASE_COLOR = 0xFFFFFFFF;

    private static final int COLOR_INFESTABLE_EP = 0x55FF55;
    private static final int COLOR_INFESTABLE_NO_EP = 0xAAAAAA;
    private static final int COLOR_TOO_STRONG = 0xFF5555;

    private MobNameplateRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob) || !mob.isAlive() || mob.isInvisibleTo(mc.player)) {
            return;
        }

        if (mc.player.getVehicle() == mob) {
            return;
        }

        Optional<MobTraits> traitsOpt = MobTraitsManager.traitsOf(mob);
        if (traitsOpt.isEmpty()) {
            return;
        }

        int range = WispbornClientConfig.NAMEPLATE_RANGE.getAsInt();
        double maxDistSqr = (double) range * range;
        if (mc.player.distanceToSqr(mob) > maxDistSqr || !mc.player.hasLineOfSight(mob)) {
            return;
        }

        float partialTick = event.getPartialTick();
        Vec3 attachment = mob.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, mob.getViewYRot(partialTick));
        if (attachment == null) {
            return;
        }

        boolean hasVanillaName = Minecraft.renderNames()
                && !mob.isVehicle()
                && (mob.shouldShowName() || (mob.hasCustomName() && mob == mc.getEntityRenderDispatcher().crosshairPickEntity));
        float yOffset = hasVanillaName ? -10.0F : 0.0F;

        MobTraits traits = traitsOpt.get();
        int statusColor = situationColor(ClientWispState.get().level(), traits.level());
        Component label = Component.translatable(
                "wispborn.nameplate.traits",
                traits.sex().glyphComponent(),
                traits.level()
        ).withStyle(Style.EMPTY.withColor(statusColor));

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(attachment.x, attachment.y + 0.5D, attachment.z);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(NAMETAG_SCALE, -NAMETAG_SCALE, NAMETAG_SCALE);

        Matrix4f matrix = poseStack.last().pose();
        float bgOpacity = mc.options.getBackgroundOpacity(0.25F);
        int bgColor = (int) (bgOpacity * 255.0F) << 24;
        boolean seeThrough = !mob.isDiscrete();
        Font font = event.getRenderer().getFont();
        float x = (float) (-font.width(label) / 2);

        font.drawInBatch(
                label,
                x,
                yOffset,
                SEE_THROUGH_BASE_COLOR,
                false,
                matrix,
                event.getMultiBufferSource(),
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                bgColor,
                event.getPackedLight()
        );
        if (seeThrough) {
            font.drawInBatch(
                    label,
                    x,
                    yOffset,
                    NORMAL_BASE_COLOR,
                    false,
                    matrix,
                    event.getMultiBufferSource(),
                    Font.DisplayMode.NORMAL,
                    0,
                    event.getPackedLight()
            );
        }

        poseStack.popPose();
    }

    private static int situationColor(int wispLevel, int mobLevel) {
        int margin = WispbornConfig.INFEST_LEVEL_GATE_MARGIN.getAsInt();
        if (mobLevel > wispLevel + margin) {
            return COLOR_TOO_STRONG;
        }
        double factor = EpMath.levelFactor(
                wispLevel,
                mobLevel,
                WispbornConfig.EP_LEVEL_GRACE.getAsInt(),
                WispbornConfig.EP_FALLOFF_PER_LEVEL.getAsDouble()
        );
        return factor <= 0.0D ? COLOR_INFESTABLE_NO_EP : COLOR_INFESTABLE_EP;
    }
}
