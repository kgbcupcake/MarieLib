package dev.marie.framework.ui;

import java.util.Optional;

/**
 * Saves/loads {@link ComponentState} keyed by component id. Deliberately knows nothing about how
 * the data is serialized or where it lives underneath — swap the implementation (e.g. a future
 * server-sync or per-world provider) without touching any MarieUI caller.
 */
public interface PersistenceProvider {

    Optional<ComponentState> load(String componentId);

    void save(String componentId, ComponentState state);
}
