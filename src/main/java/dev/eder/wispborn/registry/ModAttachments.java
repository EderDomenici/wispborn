package dev.eder.wispborn.registry;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.FormState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WispbornMod.MOD_ID);

    public static final Supplier<AttachmentType<FormState>> FORM = ATTACHMENT_TYPES.register("form",
            () -> AttachmentType.builder(() -> FormState.HUMAN)
                    .serialize(FormState.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }
}
