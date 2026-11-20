package dev.eder.wispborn.registry;

import dev.eder.wispborn.WispbornMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> CONSUMED = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "consumed")
    );

    private ModDamageTypes() {
    }
}
