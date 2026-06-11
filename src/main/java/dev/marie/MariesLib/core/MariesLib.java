package dev.marie.MariesLib.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.marie.MariesLib.config.MariesLibConfigIO;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(MariesLib.MOD_ID)
public final class MariesLib {

    public static final String MOD_ID = "marieslib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MariesLib(IEventBus modEventBus, ModContainer modContainer) {
        MariesLibConfigIO.load();
        if (!MarieLibContext.isRegistered()) {
            MariesLibBootstrap.bootstrap(modEventBus);
            // MariesLibInternalContext.get() is now the active config
        }
        LOGGER.info("MariesLib initialized");
    }
}
