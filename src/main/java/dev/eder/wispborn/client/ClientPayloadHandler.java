package dev.eder.wispborn.client;

import dev.eder.wispborn.network.SyncFormPayload;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleSyncForm(SyncFormPayload payload, IPayloadContext context) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (entity != null) {
            entity.setData(ModAttachments.FORM, payload.state());
        }
    }
}
