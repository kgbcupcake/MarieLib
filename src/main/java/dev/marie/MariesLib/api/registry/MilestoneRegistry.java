package dev.marie.MariesLib.api.registry;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MilestoneDefinition;
import dev.marie.MariesLib.registry.AbstractRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Internal storage for value milestone definitions registered via the public API.
 */
public final class MilestoneRegistry {

    private static final class Core extends AbstractRegistry<String, MilestoneDefinition> {
        Core() {
            super("MilestoneRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private MilestoneRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        INSTANCE.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        INSTANCE.reset();
    }

    /**
     * Registers a value milestone definition.
     *
     * @param definition the milestone to register
     * @throws IllegalStateException    if the registry is frozen or a milestone with the same id already exists
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void register(MilestoneDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        INSTANCE.register(definition.getId(), definition);
    }

    /**
     * Returns a registered milestone by id, or {@code null} if not found.
     *
     * @param id the milestone identifier
     * @return the milestone definition, or {@code null}
     */
    @Nullable
    public static MilestoneDefinition get(String id) {
        return INSTANCE.get(id);
    }

    /**
     * Returns all registered milestones.
     *
     * @return an unmodifiable list of all milestone definitions
     */
    public static List<MilestoneDefinition> getAll() {
        return INSTANCE.values();
    }

    /**
     * Returns all milestones that track a specific value.
     *
     * @param valueKey the value key to filter by
     * @return an unmodifiable list of matching milestones
     */
    public static List<MilestoneDefinition> getForValue(String valueKey) {
        return INSTANCE.values().stream()
                .filter(m -> valueKey.equals(m.getValueKey()))
                .toList();
    }
}
