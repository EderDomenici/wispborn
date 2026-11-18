package dev.eder.wispborn.event;

import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.network.SyncMobTraitsPayload;
import dev.eder.wispborn.registry.ModAttachments;
import dev.eder.wispborn.traits.BreedingRules;
import dev.eder.wispborn.traits.MobLevelTable;
import dev.eder.wispborn.traits.MobTraitRoller;
import dev.eder.wispborn.traits.MobTraits;
import dev.eder.wispborn.traits.MobTraitsManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TraitEvents {
    private TraitEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MobLevelTable.INSTANCE);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!mob.hasData(ModAttachments.MOB_TRAITS)) {
            MobLevelTable.INSTANCE.rangeFor(mob.getType()).ifPresent(range -> {
                MobTraits rolled = MobTraitRoller.roll(
                        range,
                        WispbornConfig.TRAITS_FEMALE_CHANCE.getAsDouble(),
                        mob.getRandom()
                );
                MobTraitsManager.set(mob, rolled);
            });
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Mob mob && event.getEntity() instanceof ServerPlayer tracker) {
            MobTraitsManager.traitsOf(mob).ifPresent(traits ->
                    PacketDistributor.sendToPlayer(
                            tracker,
                            new SyncMobTraitsPayload(mob.getId(), traits.level(), traits.sex())
                    )
            );
        }
    }

    @SubscribeEvent
    public static void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        Mob parentA = event.getParentA();
        Mob parentB = event.getParentB();
        boolean allowed = BreedingRules.canMate(
                MobTraitsManager.traitsOf(parentA).map(MobTraits::sex),
                MobTraitsManager.traitsOf(parentB).map(MobTraits::sex)
        );
        if (!allowed) {
            event.setCanceled(true);
        }
    }
}
