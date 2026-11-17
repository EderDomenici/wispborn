package dev.eder.wispborn.network;

import dev.eder.wispborn.client.ClientPayloadHandler;
import dev.eder.wispborn.host.HostManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SyncFormPayload.TYPE,
                SyncFormPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleSyncForm(payload, context)
        );
        registrar.playToClient(
                CastStatePayload.TYPE,
                CastStatePayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleCastState(payload, context)
        );
        registrar.playToClient(
                TargetInfoPayload.TYPE,
                TargetInfoPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleTargetInfo(payload, context)
        );
        registrar.playToServer(
                PossessStartPayload.TYPE,
                PossessStartPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        HostManager.handlePossessStart(serverPlayer, payload.entityId());
                    }
                }
        );
        registrar.playToServer(
                PossessCancelPayload.TYPE,
                PossessCancelPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        HostManager.cancelCast(serverPlayer, false);
                    }
                }
        );
    }
}
