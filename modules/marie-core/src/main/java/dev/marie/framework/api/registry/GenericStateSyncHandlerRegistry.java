package dev.marie.framework.api.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.network.GenericStateSyncPayload;
import dev.marie.framework.registry.ListRegistry;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Internal storage for server-side handlers registered against
 * {@link GenericStateSyncPayload} via the public API.
 */
@ApiStatus.Internal
public final class GenericStateSyncHandlerRegistry {

    private static final ListRegistry<BiConsumer<ServerPlayer, GenericStateSyncPayload>> REGISTRY =
            new ListRegistry<>("GenericStateSyncHandlerRegistry", null);

    private GenericStateSyncHandlerRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        REGISTRY.reset();
    }

    public static void register(BiConsumer<ServerPlayer, GenericStateSyncPayload> handler) {
        REGISTRY.register(handler);
    }

    public static List<BiConsumer<ServerPlayer, GenericStateSyncPayload>> getAll() {
        return REGISTRY.values();
    }
}
