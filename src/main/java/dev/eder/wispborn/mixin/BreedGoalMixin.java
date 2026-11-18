package dev.eder.wispborn.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.eder.wispborn.traits.BreedingRules;
import dev.eder.wispborn.traits.MobTraits;
import dev.eder.wispborn.traits.MobTraitsManager;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BreedGoal.class)
public abstract class BreedGoalMixin {

    @WrapOperation(
            method = "getFreePartner",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Animal;canMate(Lnet/minecraft/world/entity/animal/Animal;)Z"
            )
    )
    private boolean wispborn$requireOppositeSex(Animal self, Animal other, Operation<Boolean> original) {
        if (!original.call(self, other)) {
            return false;
        }
        return BreedingRules.canMate(
                MobTraitsManager.traitsOf(self).map(MobTraits::sex),
                MobTraitsManager.traitsOf(other).map(MobTraits::sex)
        );
    }
}
