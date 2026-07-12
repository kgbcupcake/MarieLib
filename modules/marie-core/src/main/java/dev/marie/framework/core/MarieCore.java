package dev.marie.framework.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.marie.framework.config.MariesLibConfigIO;
import dev.marie.framework.network.BlockHoverClientNetworking;
import dev.marie.framework.network.BlockHoverServerNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(MarieCore.MOD_ID)
public final class MarieCore {

    public static final String MOD_ID = "marieslib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MarieCore(IEventBus modEventBus, ModContainer modContainer) {
        MariesLibConfigIO.load();
        modEventBus.addListener(BlockHoverServerNetworking::register);
        modEventBus.addListener(BlockHoverClientNetworking::register);
        LOGGER.info("MarieCore initialized");
    }
}
