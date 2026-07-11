package dev.marie.framework.ui.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class ModuleRegistry {

    private static final Map<String, List<ModuleFactory<?>>> MODULES = new LinkedHashMap<>();

    private ModuleRegistry() {}

    public static synchronized void register(String modId, ModuleFactory<?> factory) {
        MODULES.computeIfAbsent(modId, k -> new ArrayList<>()).add(factory);
    }

    /** Every module factory registered for {@code modId}, in registration order. Empty if none. */
    public static synchronized List<ModuleFactory<?>> get(String modId) {
        return Collections.unmodifiableList(MODULES.getOrDefault(modId, List.of()));
    }
}
