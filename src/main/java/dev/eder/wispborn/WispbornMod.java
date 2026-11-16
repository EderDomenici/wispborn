package dev.eder.wispborn;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(WispbornMod.MOD_ID)
public class WispbornMod {
    public static final String MOD_ID = "wispborn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WispbornMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Inicializando Wispborn ({})", MOD_ID);
    }
}
