package dev.eder.wispborn.form;

import dev.eder.wispborn.registry.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WispEssenceItem extends Item {
    public WispEssenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            FormState current = serverPlayer.getData(ModAttachments.FORM);
            FormState next = current.type() == FormType.HUMAN ? FormState.WISP : FormState.HUMAN;
            FormManager.setForm(serverPlayer, next);

            serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    serverPlayer.getX(), serverPlayer.getY() + 0.5, serverPlayer.getZ(),
                    16, 0.25, 0.25, 0.25, 0.04);

            stack.consume(1, serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
