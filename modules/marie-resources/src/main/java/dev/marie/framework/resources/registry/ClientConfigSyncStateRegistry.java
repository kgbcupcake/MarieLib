package dev.marie.framework.resources.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.network.SyncState;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side {@link SyncState} tracking per registryId, updated as
 * {@link dev.marie.framework.resources.network.GenericConfigSyncPayload} instances arrive.
 * Unlike the supplier/handler registries this is mutated continuously at runtime, not just
 * during registration, so it is a plain concurrent map rather than an {@code AbstractRegistry}.
 */
@ApiStatus.Internal
public final class ClientConfigSyncStateRegistry {

    private static final ConcurrentHashMap<String, SyncState> STATES = new ConcurrentHashMap<>();

    private ClientConfigSyncStateRegistry() {}

    public static SyncState get(String registryId) {
        return STATES.getOrDefault(registryId, SyncState.UNINITIALIZED);
    }

    public static void set(String registryId, SyncState state) {
        STATES.put(registryId, state);
    }
}
