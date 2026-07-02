package dev.marie.MariesLib.runtime;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.SourceTriggerDefinition;
import dev.marie.MariesLib.api.ValueSourceTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores SourceTriggerDefinitions registered via the public API.
 * Keyed by (triggerType + sourceKey) → list of (valueKey, amount) pairs.
 */
@ApiStatus.Internal
public final class SourceTriggerRegistry {

    private static final ConcurrentHashMap<String, List<Entry>> REGISTRY = new ConcurrentHashMap<>();

    private SourceTriggerRegistry() {}

    public record Entry(String valueKey, float amount) {}

    public static void register(SourceTriggerDefinition def) {
        String key = def.getTriggerType().name() + ":" + def.getSourceKey();
        REGISTRY.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new Entry(def.getValueKey(), def.getAmount()));
    }

    /**
     * Returns all registered entries for this trigger, or an empty list.
     */
    public static List<Entry> getEntries(ValueSourceTrigger.TriggerType type, String sourceKey) {
        String key = type.name() + ":" + sourceKey;
        return REGISTRY.getOrDefault(key, List.of());
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
