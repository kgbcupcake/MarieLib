package dev.marie.framework.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.color.ColorDefinitionRegistry;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.data.MarieDataManager;
import dev.marie.framework.registry.RegistryLifecycleManager;
import dev.marie.framework.tracking.tracker.registry.TrackerRegistry;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
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

    /**
     * Invokes {@link MarieContext#reloadBroadcastHook()}, the "reload happened, please
     * re-register your trackers/colors" hook. Called automatically after every resource reload
     * (vanilla {@code /reload} or a mod's own reload command) via {@link #onDatapackSync}; exposed
     * here for callers that trigger a reload through a path this listener cannot observe.
     *
     * <p>{@code TrackerRegistry}/{@code ColorDefinitionRegistry} are frozen by the time this runs
     * (see {@link dev.marie.framework.registry.MarieApiRegistries#onDatapackApplyEnd}), so both are
     * briefly unfrozen for the duration of the hook call to allow {@code registerTracker}/
     * {@code registerColor} re-registration, then refrozen in a {@code finally} block regardless of
     * whether the hook throws.</p>
     */
    public static void reloadAndBroadcast(MinecraftServer server) {
        if (MarieContext.isRegistered()) {
            TrackerRegistry.unfreezeInternal();
            ColorDefinitionRegistry.unfreezeInternal();
            try {
                MarieContext.get().reloadBroadcastHook().accept(server);
            } finally {
                TrackerRegistry.freezeInternal();
                ColorDefinitionRegistry.freezeInternal();
            }
        }
    }

    /**
     * Fires after every resource reload completes — {@code event.getPlayer() == null} indicates a
     * full reload (vanilla {@code /reload} or a mod's own reload command), as opposed to a single
     * player's join-time datapack sync. This runs strictly after {@link MinecraftServer#reloadResources}'s
     * returned future resolves, which is after {@code MarieApiRegistries}' reload-scoped resets
     * ({@code TrackerRegistry}/{@code ColorDefinitionRegistry} included) have already completed —
     * making this the safe point for consumers to re-register.
     */
    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        reloadAndBroadcast(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        MarieDataManager.registerReloadListener(event);
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) ->
                preparationBarrier.wait(net.minecraft.util.Unit.INSTANCE).thenRunAsync(() -> {
                    reloadInProgress = true;
                    try {
                        RegistryLifecycleManager.loadAll(resourceManager);
                        MarieCore.LOGGER.info("[MarieLib] Datapack config reload complete");
                    } finally {
                        reloadInProgress = false;
                    }
                }, executor2)
        );
    }
}
