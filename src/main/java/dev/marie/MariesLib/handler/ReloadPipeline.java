package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.diagnostics.MarieUnknownItemLogger;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import dev.marie.MariesLib.runtime.RuntimeResolver;
import dev.marie.MariesLib.runtime.SourceTriggerRegistry;
import dev.marie.MariesLib.scanner.ItemScanner;

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
