package dev.eder.wispborn.client;

import dev.eder.wispborn.network.CastStatePayload;
import dev.eder.wispborn.network.SyncFormPayload;
import dev.eder.wispborn.network.SyncMobTraitsPayload;
import dev.eder.wispborn.network.TargetInfoPayload;
import dev.eder.wispborn.registry.ModAttachments;
import dev.eder.wispborn.traits.MobTraits;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
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

    public static void handleCastState(CastStatePayload payload, IPayloadContext context) {
        ClientPossession.setCastState(payload);
    }

    public static void handleTargetInfo(TargetInfoPayload payload, IPayloadContext context) {
        ClientPossession.setTargetInfo(payload);
    }

    public static void handleSyncMobTraits(SyncMobTraitsPayload payload, IPayloadContext context) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (entity instanceof Mob mob) {
            mob.setData(ModAttachments.MOB_TRAITS, new MobTraits(payload.level(), payload.sex()));
        }
    }
}
