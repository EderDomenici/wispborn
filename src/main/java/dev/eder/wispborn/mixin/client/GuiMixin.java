package dev.eder.wispborn.mixin.client;

import dev.eder.wispborn.form.FormType;
import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O hospedeiro não é montaria: dentro dele o HUD vanilla mantém a fome do wisp e não mostra os
 * corações do mob. As duas camadas (FOOD_LEVEL e VEHICLE_HEALTH) decidem isso por este método.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "getPlayerVehicleWithHealth", at = @At("HEAD"), cancellable = true)
    private void wispborn$hostIsNotAMount(CallbackInfoReturnable<LivingEntity> cir) {
        if (Minecraft.getInstance().getCameraEntity() instanceof Player player
                && player.getData(ModAttachments.FORM).type() == FormType.HOST) {
            cir.setReturnValue(null);
        }
    }
}
