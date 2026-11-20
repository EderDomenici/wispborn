package dev.eder.wispborn.client.hud;

import dev.eder.wispborn.WispbornMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Posição vertical do slot do topo central (painel do alvo e do hospedeiro). As barras de boss também
 * ficam no topo central; quando há alguma, o slot desce para logo abaixo da última. A camada de boss é
 * desenhada depois das nossas, então usamos a medida do quadro anterior (um quadro de atraso).
 */
@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class TopSlot {
    private static final int DEFAULT_TOP = 2;
    /** Altura da barra de boss abaixo do Y informado pelo evento (5 px de barra + 2 de folga). */
    private static final int BOSS_BAR_HEIGHT = 7;

    private static int bossBottomThisFrame = 0;
    private static int bossBottomLastFrame = 0;

    private TopSlot() {
    }

    public static int top() {
        return HudMath.topSlotY(DEFAULT_TOP, bossBottomLastFrame);
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        bossBottomThisFrame = Math.max(bossBottomThisFrame, event.getY() + BOSS_BAR_HEIGHT);
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        bossBottomLastFrame = bossBottomThisFrame;
        bossBottomThisFrame = 0;
    }
}
