package dev.eder.wispborn.form;

import dev.eder.wispborn.network.SyncFormPayload;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FormManager {
    private FormManager() {
    }

    public static void setForm(ServerPlayer player, FormState state) {
        float previousMaxHealth = player.getMaxHealth();
        float healthRatio = previousMaxHealth > 0.0F ? player.getHealth() / previousMaxHealth : 1.0F;
        player.setData(ModAttachments.FORM, state);
        applyModifiers(player);
        float newMaxHealth = player.getMaxHealth();
        if (newMaxHealth != previousMaxHealth && player.getHealth() > 0.0F) {
            player.setHealth(Math.clamp(healthRatio * newMaxHealth, 0.01F, newMaxHealth));
        }
        sync(player);
    }

    public static void applyModifiers(ServerPlayer player) {
        removeModifier(player, Attributes.SCALE, WispTuning.SCALE_ID);
        removeModifier(player, Attributes.MAX_HEALTH, WispTuning.MAX_HEALTH_ID);
        removeModifier(player, Attributes.GRAVITY, WispTuning.GRAVITY_ID);
        removeModifier(player, Attributes.SAFE_FALL_DISTANCE, WispTuning.SAFE_FALL_DISTANCE_ID);
        removeModifier(player, Attributes.MOVEMENT_SPEED, WispTuning.MOVEMENT_SPEED_ID);

        if (isParasite(player)) {
            addModifier(player, Attributes.SCALE, WispTuning.SCALE_ID,
                    WispTuning.SCALE_MODIFIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addModifier(player, Attributes.MAX_HEALTH, WispTuning.MAX_HEALTH_ID,
                    WispTuning.MAX_HEALTH_MODIFIER, AttributeModifier.Operation.ADD_VALUE);
            addModifier(player, Attributes.GRAVITY, WispTuning.GRAVITY_ID,
                    WispTuning.GRAVITY_MODIFIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addModifier(player, Attributes.SAFE_FALL_DISTANCE, WispTuning.SAFE_FALL_DISTANCE_ID,
                    WispTuning.SAFE_FALL_DISTANCE_MODIFIER, AttributeModifier.Operation.ADD_VALUE);
            addModifier(player, Attributes.MOVEMENT_SPEED, WispTuning.MOVEMENT_SPEED_ID,
                    WispTuning.MOVEMENT_SPEED_MODIFIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    public static void sync(ServerPlayer player) {
        FormState state = player.getData(ModAttachments.FORM);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SyncFormPayload(player.getId(), state));
    }

    public static boolean isWisp(Player player) {
        return player.getData(ModAttachments.FORM).type() == FormType.WISP;
    }

    public static boolean isParasite(Player player) {
        FormType type = player.getData(ModAttachments.FORM).type();
        return type == FormType.WISP || type == FormType.HOST;
    }

    private static void addModifier(ServerPlayer player, Holder<Attribute> attribute,
                                    ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
