package dev.marie.framework.api.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.hover.BlockHoverProvider;
import dev.marie.framework.registry.ListRegistry;

import java.util.List;

/**
 * Internal storage for tracking block-hover providers registered via the public API.
 */
@ApiStatus.Internal
public final class BlockHoverProviderRegistry {

    private static final ListRegistry<BlockHoverProvider> REGISTRY =
            new ListRegistry<>("BlockHoverProviderRegistry", null);

    private BlockHoverProviderRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        REGISTRY.reset();
    }

    /**
     * Registers a block-hover provider.
     *
     * @param provider the provider to register
     * @throws IllegalArgumentException if {@code provider} is null
     */
    public static void register(BlockHoverProvider provider) {
        REGISTRY.register(provider);
    }

    /**
     * Returns all registered block-hover providers in registration order.
     *
     * @return an unmodifiable list of providers
     */
    public static List<BlockHoverProvider> getAll() {
        return REGISTRY.values();
    }
}
