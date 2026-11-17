package dev.eder.wispborn.event;

import dev.eder.wispborn.form.FormManager;
import dev.eder.wispborn.registry.ModItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class WispRestrictions {
    private WispRestrictions() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (FormManager.isParasite(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (FormManager.isParasite(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (FormManager.isParasite(event.getEntity())) {
            boolean canUseEssence = FormManager.isWisp(event.getEntity())
                    && event.getItemStack().is(ModItems.WISP_ESSENCE.get());
            if (!canUseEssence) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (FormManager.isParasite(event.getEntity())) {
            boolean canUseEssence = FormManager.isWisp(event.getEntity())
                    && event.getItemStack().is(ModItems.WISP_ESSENCE.get());
            if (canUseEssence) {
                event.setUseBlock(TriState.FALSE);
            } else {
                event.setCanceled(true);
            }
        }
    }
}
