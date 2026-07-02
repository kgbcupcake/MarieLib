package dev.marie.framework.api.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ReportProvider;
import dev.marie.framework.registry.ListRegistry;

import java.util.List;

/**
 * Internal storage for tracking report providers registered via the public API.
 */
@ApiStatus.Internal
public final class ReportProviderRegistry {

    private static final ListRegistry<ReportProvider> REGISTRY = new ListRegistry<>("ReportProviderRegistry", null);

    private ReportProviderRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    /**
     * Registers a tracking report provider.
     *
     * @param provider the report provider to register
     * @throws IllegalArgumentException if {@code provider} is null
     */
    public static void register(ReportProvider provider) {
        REGISTRY.register(provider);
    }

    /**
     * Returns all registered report providers in registration order.
     *
     * @return an unmodifiable list of report providers
     */
    public static List<ReportProvider> getAll() {
        return REGISTRY.values();
    }
}
