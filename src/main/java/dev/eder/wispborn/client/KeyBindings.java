package dev.eder.wispborn.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.host.HostManager;
import dev.eder.wispborn.host.InfestResult;
import dev.eder.wispborn.network.ConsumeRequestPayload;
import dev.eder.wispborn.network.PossessCancelPayload;
import dev.eder.wispborn.network.PossessStartPayload;
import dev.eder.wispborn.network.TargetInfoPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.wispborn";

    public static final KeyMapping POSSESS = new KeyMapping(
            "key.wispborn.possess",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping CONSUME = new KeyMapping(
            "key.wispborn.consume",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    private static boolean holdActive = false;

    private KeyBindings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(POSSESS);
        event.register(CONSUME);
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player localPlayer = mc.player;
        if (localPlayer == null || mc.screen != null) {
            if (holdActive || ClientPossession.isCasting()) {
                PacketDistributor.sendToServer(PossessCancelPayload.INSTANCE);
                ClientPossession.clearCast();
                holdActive = false;
            }
            return;
        }

        while (CONSUME.consumeClick()) {
            if (HostManager.isHost(localPlayer)) {
                PacketDistributor.sendToServer(ConsumeRequestPayload.INSTANCE);
            }
        }

        if (!FormManager.isWisp(localPlayer)) {
            if (holdActive || ClientPossession.isCasting()) {
                PacketDistributor.sendToServer(PossessCancelPayload.INSTANCE);
                ClientPossession.clearCast();
                holdActive = false;
            }
            return;
        }

        if (POSSESS.isDown()) {
            if (!holdActive && !ClientPossession.isCasting()) {
                TargetInfoPayload target = ClientPossession.targetInfo();
                if (target.entityId() != -1 && target.result() == InfestResult.OK) {
                    PacketDistributor.sendToServer(new PossessStartPayload(target.entityId()));
                    holdActive = true;
                }
            }
        } else {
            if (holdActive || ClientPossession.isCasting()) {
                PacketDistributor.sendToServer(PossessCancelPayload.INSTANCE);
                ClientPossession.clearCast();
                holdActive = false;
            }
        }
    }
}
