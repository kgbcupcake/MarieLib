package dev.marie.framework.api.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.source.SourcePropertySignal;
import dev.marie.framework.registry.ListRegistry;

import java.util.List;

@ApiStatus.Internal
public final class SourcePropertySignalRegistry {

    private static final ListRegistry<SourcePropertySignal> REGISTRY =
            new ListRegistry<>("SourcePropertySignalRegistry", null);

    private SourcePropertySignalRegistry() {}

    public static void register(SourcePropertySignal signal) {
        REGISTRY.register(signal);
    }

    public static List<SourcePropertySignal> getAll() {
        return REGISTRY.values();
    }

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        REGISTRY.reset();
    }
}
