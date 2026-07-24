package dev.marie.framework.network;

import dev.marie.framework.api.ApiStatus;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of per-entity values of type {@code T}, keyed by entity id.
 * Populated by the consumer's clientbound payload handler; read on the render thread.
 * Backed by {@link ConcurrentHashMap} since network and render threads both touch it.
 */
@ApiStatus.Experimental
public final class ClientEntityValueCache<T> {

    private final ConcurrentHashMap<Integer, T> cache = new ConcurrentHashMap<>();

    public void put(int entityId, T value) {
        cache.put(entityId, value);
    }

    public T get(int entityId) {
        return cache.get(entityId);
    }

    public T getOrDefault(int entityId, T fallback) {
        return cache.getOrDefault(entityId, fallback);
    }

    public boolean contains(int entityId) {
        return cache.containsKey(entityId);
    }

    public void remove(int entityId) {
        cache.remove(entityId);
    }

    /** Clears all entries, e.g. on disconnect or dimension change. */
    public void clear() {
        cache.clear();
    }
}
