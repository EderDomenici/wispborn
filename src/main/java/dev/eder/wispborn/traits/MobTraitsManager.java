package dev.eder.wispborn.traits;

import dev.eder.wispborn.network.SyncMobTraitsPayload;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public final class MobTraitsManager {
    private MobTraitsManager() {
    }

    public static Optional<MobTraits> traitsOf(Mob mob) {
        return mob.getExistingData(ModAttachments.MOB_TRAITS);
    }

    public static void set(Mob mob, MobTraits traits) {
        mob.setData(ModAttachments.MOB_TRAITS, traits);
        if (!mob.level().isClientSide()) {
            PacketDistributor.sendToPlayersTrackingEntity(
                    mob,
                    new SyncMobTraitsPayload(mob.getId(), traits.level(), traits.sex())
            );
        }
    }
}
