package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.diagnostics.MarieUnknownItemLogger;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;

@ApiStatus.Internal
public final class ReloadPipeline {

    private ReloadPipeline() {}

    public static void reloadAll() {
        RegistryLifecycleManager.reloadAll();
        MarieLibContext.get().onCacheInvalidated();
        MarieUnknownItemLogger.onReload();
    }
}
