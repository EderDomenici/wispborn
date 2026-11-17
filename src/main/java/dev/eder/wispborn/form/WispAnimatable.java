package dev.eder.wispborn.form;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class WispAnimatable implements GeoReplacedEntity {
    public static final WispAnimatable INSTANCE = new WispAnimatable();

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop(WispAnimations.IDLE);
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop(WispAnimations.WALK);
    private static final RawAnimation POSSESS_ANIM = RawAnimation.begin().thenPlay(WispAnimations.POSSESS);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private WispAnimatable() {
    }

    @Override
    public EntityType<?> getReplacingEntityType() {
        return EntityType.PLAYER;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object entity) {
        return ((Entity) entity).tickCount;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, WispAnimations.CONTROLLER_MOVEMENT, 5,
                state -> state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, WispAnimations.CONTROLLER_POSSESS, 0,
                state -> PlayState.STOP)
                .triggerableAnim(WispAnimations.TRIGGER_POSSESS, POSSESS_ANIM));
    }

    public static void playPossess(ServerPlayer player) {
        INSTANCE.triggerAnim(player, WispAnimations.CONTROLLER_POSSESS, WispAnimations.TRIGGER_POSSESS);
    }
}
