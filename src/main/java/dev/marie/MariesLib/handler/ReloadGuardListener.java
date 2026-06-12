package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.data.MarieDataManager;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@ApiStatus.Internal
public class ReloadGuardListener {

    private static volatile boolean reloadInProgress;

    public static boolean isReloadInProgress() {
        return reloadInProgress || RegistryLifecycleManager.isReloadInProgress();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        reloadInProgress = true;
        try {
            ReloadPipeline.reloadAll();
        } finally {
            reloadInProgress = false;
        }
    }

    public static void reloadAndBroadcast(MinecraftServer server) {
        if (MarieLibContext.isRegistered()) {
            MarieLibContext.get().reloadBroadcastHook().accept(server);
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        MarieDataManager.registerReloadListener(event);
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) ->
                preparationBarrier.wait(net.minecraft.util.Unit.INSTANCE).thenRunAsync(() -> {
                    reloadInProgress = true;
                    try {
                        RegistryLifecycleManager.loadAll(resourceManager);
                        MariesLib.LOGGER.info("[MarieLib] Datapack config reload complete");
                    } finally {
                        reloadInProgress = false;
                    }
                }, executor2)
        );
    }
}
