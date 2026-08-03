package dev.marie.framework.tracking.tracker.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.registry.AbstractRegistry;
import dev.marie.framework.tracking.tracker.definition.TrackerDefinition;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Internal storage for tracker definitions registered via the public API.
 */
@ApiStatus.Internal
public final class TrackerRegistry {

    private static final class Core extends AbstractRegistry<ResourceLocation, TrackerDefinition> {
        Core() {
            super("TrackerRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private TrackerRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        INSTANCE.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        INSTANCE.reset();
    }

    public static boolean isFrozen() {
        return INSTANCE.isFrozen();
    }

    /**
     * Registers a tracker definition, enforcing the configured retention cap.
     *
     * @throws IllegalArgumentException if {@code definition} is null, retention is less than 1,
     *                                   or retention exceeds {@code IMarieConfig#trackerMaxRetention()}
     */
    public static void register(TrackerDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        int retention = definition.getRetention();
        int maxRetention = IMarieConfig.get().trackerMaxRetention();
        if (retention < 1) {
            throw new IllegalArgumentException(
                    "TrackerDefinition '" + definition.getId() + "': retention must be >= 1, got " + retention);
        }
        if (retention > maxRetention) {
            throw new IllegalArgumentException(
                    "TrackerDefinition '" + definition.getId() + "': retention " + retention +
                    " exceeds trackerMaxRetention " + maxRetention);
        }
        INSTANCE.register(definition.getId(), definition);
    }

    @Nullable
    public static TrackerDefinition get(ResourceLocation id) {
        return INSTANCE.get(id);
    }

    public static List<TrackerDefinition> getAll() {
        return INSTANCE.values();
    }
}
