package dev.marie.framework.resources.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.registry.AbstractRegistry;

import net.minecraft.nbt.CompoundTag;

import java.util.function.Supplier;

/**
 * Server-side storage for per-registryId snapshot suppliers registered via
 * {@link dev.marie.framework.resources.api.MarieResourcesAPI#registerConfigSyncSupplier}.
 */
@ApiStatus.Internal
public final class ConfigSyncSupplierRegistry extends AbstractRegistry<String, Supplier<CompoundTag>> {

    private static final ConfigSyncSupplierRegistry INSTANCE = new ConfigSyncSupplierRegistry();

    private ConfigSyncSupplierRegistry() {
        super("ConfigSyncSupplierRegistry");
    }

    public static ConfigSyncSupplierRegistry instance() {
        return INSTANCE;
    }
}
