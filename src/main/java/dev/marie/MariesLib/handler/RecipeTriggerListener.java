package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.scanner.ItemScanner;
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
