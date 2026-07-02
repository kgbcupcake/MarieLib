package dev.marie.framework.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.scanner.ItemScanner;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@ApiStatus.Internal
public final class RecipeTriggerListener {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ItemScanner.scanAndApply(event.getServer().getRecipeManager());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ItemScanner.invalidateCache();
    }
}
