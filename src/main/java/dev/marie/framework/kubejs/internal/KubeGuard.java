package dev.marie.framework.kubejs.internal;

import dev.latvian.mods.kubejs.event.EventHandler;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.kubejs.MarieKubeEvents;
import net.neoforged.fml.ModList;

import java.util.concurrent.ConcurrentHashMap;

/**
 * KubeJS presence check and per-event listener cache.
 */
@ApiStatus.Internal
public final class KubeGuard {

    private static final boolean PRESENT = ModList.get().isLoaded("kubejs");

    private static final ConcurrentHashMap<String, Boolean> LISTENER_CACHE = new ConcurrentHashMap<>();

    private KubeGuard() {}

    public static boolean isPresent() {
        return PRESENT;
    }

    public static boolean hasListeners(String eventId) {
        if (!PRESENT) {
            return false;
        }
        return LISTENER_CACHE.computeIfAbsent(eventId, KubeGuard::resolveHasListeners);
    }

    private static boolean resolveHasListeners(String eventId) {
        EventHandler handler = MarieKubeEvents.handlerFor(eventId);
        return handler != null && handler.hasListeners();
    }

    public static void invalidateCache() {
        LISTENER_CACHE.clear();
    }
}
