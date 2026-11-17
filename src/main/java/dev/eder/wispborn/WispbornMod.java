package dev.eder.wispborn;

import com.mojang.logging.LogUtils;
import dev.eder.wispborn.command.WispbornCommand;
import dev.eder.wispborn.config.WispbornConfig;
import dev.eder.wispborn.event.FormEvents;
import dev.eder.wispborn.event.HostEvents;
import dev.eder.wispborn.event.WispRestrictions;
import dev.eder.wispborn.event.WispSounds;
import dev.eder.wispborn.form.WispAnimatable;
import dev.eder.wispborn.network.ModPayloads;
import dev.eder.wispborn.registry.ModAttachments;
import dev.eder.wispborn.registry.ModItems;
import dev.eder.wispborn.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;

@Mod(WispbornMod.MOD_ID)
public class WispbornMod {
    public static final String MOD_ID = "wispborn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WispbornMod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, WispbornConfig.SPEC);

        SingletonGeoAnimatable.registerSyncedAnimatable(WispAnimatable.INSTANCE);

        modEventBus.addListener(ModItems::addCreative);
        modEventBus.addListener(ModPayloads::register);

        NeoForge.EVENT_BUS.register(FormEvents.class);
        NeoForge.EVENT_BUS.register(HostEvents.class);
        NeoForge.EVENT_BUS.register(WispRestrictions.class);
        NeoForge.EVENT_BUS.register(WispSounds.class);
        NeoForge.EVENT_BUS.register(WispbornCommand.class);

        LOGGER.info("Inicializando Wispborn ({})", MOD_ID);
    }
}
