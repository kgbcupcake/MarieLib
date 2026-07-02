package dev.marie.framework.runtime;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.SourceTriggerListener;
import dev.marie.framework.registry.ListRegistry;

import java.util.List;

@ApiStatus.Internal
public final class TriggerHandlerRegistry {

    private static final ListRegistry<SourceTriggerListener> REGISTRY =
            new ListRegistry<>("TriggerHandlerRegistry", null);

    private TriggerHandlerRegistry() {}

    public static void register(SourceTriggerListener handler) {
        REGISTRY.register(handler);
    }

    public static List<SourceTriggerListener> getAll() {
        return REGISTRY.values();
    }

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }
}
