package dev.eder.wispborn.registry;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.WispEssenceItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WispbornMod.MOD_ID);

    public static final DeferredItem<Item> WISP_ESSENCE = ITEMS.register("wisp_essence",
            () -> new WispEssenceItem(new Item.Properties().stacksTo(16)));

    private ModItems() {
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(WISP_ESSENCE);
        }
    }
}
