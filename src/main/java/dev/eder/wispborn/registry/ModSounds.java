package dev.eder.wispborn.registry;

import dev.eder.wispborn.WispbornMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, WispbornMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> WISP_DRIFT = SOUND_EVENTS.register(
            "entity.wisp.drift",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "entity.wisp.drift")
            )
    );

    /** Pulso da drenagem do hospedeiro. Aponta para um som vanilla em sounds.json; a arte pode trocar. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_DRAIN_PULSE = SOUND_EVENTS.register(
            "host.drain_pulse",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "host.drain_pulse")
            )
    );

    private ModSounds() {
    }
}
