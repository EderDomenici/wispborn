package dev.eder.wispborn.registry;

import dev.eder.wispborn.WispbornMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    public static final TagKey<EntityType<?>> TIER_1 = create("tier_1");
    public static final TagKey<EntityType<?>> TIER_2 = create("tier_2");
    public static final TagKey<EntityType<?>> TIER_3 = create("tier_3");

    private ModTags() {
    }

    private static TagKey<EntityType<?>> create(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, name));
    }
}
