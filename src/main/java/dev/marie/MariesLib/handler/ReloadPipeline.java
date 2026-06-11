package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.diagnostics.MarieUnknownItemLogger;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import dev.marie.MariesLib.runtime.SourceTriggerRegistry;

@ApiStatus.Internal
public final class ReloadPipeline {

    private ReloadPipeline() {}

    public static void reloadAll() {
        RegistryLifecycleManager.reloadAll();
        SourceTriggerRegistry.clear();
        MarieLibContext.get().onCacheInvalidated();
        MarieUnknownItemLogger.onReload();
    }
}
