package dev.marie.framework.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.diagnostics.MarieUnknownItemLogger;
import dev.marie.framework.registry.RegistryLifecycleManager;
import dev.marie.framework.runtime.RuntimeResolver;
import dev.marie.framework.runtime.SourceTriggerRegistry;
import dev.marie.framework.scanner.ItemScanner;

@ApiStatus.Internal
public final class ReloadPipeline {

    private ReloadPipeline() {}

    public static void reloadAll() {
        RegistryLifecycleManager.reloadAll();
        SourceTriggerRegistry.clear();
        ItemScanner.invalidateCache();
        RuntimeResolver.getInstance().invalidateCache();
        MarieUnknownItemLogger.onReload();
    }
}
