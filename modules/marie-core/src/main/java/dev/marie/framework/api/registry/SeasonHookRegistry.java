package dev.marie.framework.api.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marie.MarieSeasonHook;
import dev.marie.framework.registry.ListRegistry;

import java.util.List;

/**
 * Internal storage for season hooks registered via the public API.
 */
@ApiStatus.Internal
public final class SeasonHookRegistry {

    private static final ListRegistry<MarieSeasonHook> REGISTRY = new ListRegistry<>("SeasonHookRegistry", null);

    private SeasonHookRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    /**
     * Registers a season hook.
     *
     * @param hook the season hook to register
     * @throws IllegalArgumentException if {@code hook} is null
     */
    public static void register(MarieSeasonHook hook) {
        REGISTRY.register(hook);
    }

    /**
     * Returns all registered season hooks.
     *
     * @return an unmodifiable list of season hooks
     */
    public static List<MarieSeasonHook> getAll() {
        return REGISTRY.values();
    }
}
