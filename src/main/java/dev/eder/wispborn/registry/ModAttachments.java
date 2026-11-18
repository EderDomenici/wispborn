package dev.eder.wispborn.registry;

import com.mojang.serialization.Codec;
import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormState;
import dev.eder.wispborn.progress.WispProgress;
import dev.eder.wispborn.progress.WispStats;
import dev.eder.wispborn.traits.MobTraits;
import dev.eder.wispborn.traits.Sex;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WispbornMod.MOD_ID);

    public static final Supplier<AttachmentType<FormState>> FORM = ATTACHMENT_TYPES.register("form",
            () -> AttachmentType.builder(() -> FormState.WISP)
                    .serialize(FormState.CODEC)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<WispProgress>> PROGRESS = ATTACHMENT_TYPES.register("progress",
            () -> AttachmentType.builder(() -> WispProgress.START)
                    .serialize(WispProgress.CODEC)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<WispStats>> STATS = ATTACHMENT_TYPES.register("stats",
            () -> AttachmentType.builder(() -> WispStats.ZERO)
                    .serialize(WispStats.CODEC)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<Float>> MANA = ATTACHMENT_TYPES.register("mana",
            () -> AttachmentType.builder(() -> 50.0F)
                    .serialize(Codec.FLOAT)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<MobTraits>> MOB_TRAITS = ATTACHMENT_TYPES.register("mob_traits",
            () -> AttachmentType.builder(() -> new MobTraits(1, Sex.MALE))
                    .serialize(MobTraits.CODEC)
                    .build());

    private ModAttachments() {
    }
}
