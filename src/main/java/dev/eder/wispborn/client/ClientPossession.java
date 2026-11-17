package dev.eder.wispborn.client;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.network.CastStatePayload;
import dev.eder.wispborn.network.TargetInfoPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = WispbornMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPossession {
    private static TargetInfoPayload targetInfo = TargetInfoPayload.NONE;
    private static CastStatePayload castState = CastStatePayload.NONE;
    private static long castStartGameTime = 0L;

    private ClientPossession() {
    }

    public static TargetInfoPayload targetInfo() {
        return targetInfo;
    }

    public static void setTargetInfo(TargetInfoPayload payload) {
        targetInfo = payload;
    }

    public static CastStatePayload castState() {
        return castState;
    }

    public static long castStartGameTime() {
        return castStartGameTime;
    }

    public static boolean isCasting() {
        return castState.targetEntityId() != -1 && castState.durationTicks() > 0;
    }

    public static void setCastState(CastStatePayload payload) {
        castState = payload;
        Level level = Minecraft.getInstance().level;
        castStartGameTime = (level != null && payload.targetEntityId() != -1) ? level.getGameTime() : 0L;
    }

    public static void clearCast() {
        castState = CastStatePayload.NONE;
        castStartGameTime = 0L;
    }

    public static void reset() {
        targetInfo = TargetInfoPayload.NONE;
        clearCast();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }
}
