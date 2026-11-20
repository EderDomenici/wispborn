package dev.eder.wispborn.client;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.network.SyncWispStatePayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class ClientWispState {
    private static SyncWispStatePayload state = SyncWispStatePayload.DEFAULT;

    private ClientWispState() {
    }

    public static SyncWispStatePayload get() {
        return state;
    }

    public static void set(SyncWispStatePayload payload) {
        state = payload;
    }

    public static void reset() {
        state = SyncWispStatePayload.DEFAULT;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }
}
