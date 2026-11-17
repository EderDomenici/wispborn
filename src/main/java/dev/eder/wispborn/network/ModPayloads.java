package dev.eder.wispborn.network;

import dev.eder.wispborn.client.ClientPayloadHandler;
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
    }
}
