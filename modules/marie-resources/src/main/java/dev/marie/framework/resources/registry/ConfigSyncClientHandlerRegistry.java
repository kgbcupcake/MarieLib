package dev.marie.framework.resources.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.registry.AbstractRegistry;

import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

/**
 * Client-side storage for per-registryId apply functions registered via
 * {@link dev.marie.framework.resources.api.MarieResourcesAPI#registerConfigSyncClientHandler}.
 */
@ApiStatus.Internal
public final class ConfigSyncClientHandlerRegistry extends AbstractRegistry<String, Consumer<CompoundTag>> {

    private static final ConfigSyncClientHandlerRegistry INSTANCE = new ConfigSyncClientHandlerRegistry();

    private ConfigSyncClientHandlerRegistry() {
        super("ConfigSyncClientHandlerRegistry");
    }

    public static ConfigSyncClientHandlerRegistry instance() {
        return INSTANCE;
    }
}
