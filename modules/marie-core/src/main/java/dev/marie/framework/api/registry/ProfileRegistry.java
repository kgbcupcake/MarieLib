package dev.marie.framework.api.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ProfileDefinition;
import dev.marie.framework.registry.AbstractRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Internal storage for tracking profile definitions registered via the public API.
 */
@ApiStatus.Internal
public final class ProfileRegistry {

    private static final class Core extends AbstractRegistry<String, ProfileDefinition> {
        Core() {
            super("ProfileRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private ProfileRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        INSTANCE.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        INSTANCE.reset();
    }

    /**
     * Registers a tracking profile definition.
     *
     * @param definition the tracking profile to register
     * @throws IllegalStateException    if the registry is frozen or a profile with the same id already exists
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void register(ProfileDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        INSTANCE.register(definition.getId(), definition);
    }

    /**
     * Returns a registered tracking profile by id, or {@code null} if not found.
     *
     * @param id the profile identifier
     * @return the profile definition, or {@code null}
     */
    @Nullable
    public static ProfileDefinition get(String id) {
        return INSTANCE.get(id);
    }

    /**
     * Returns all registered tracking profiles.
     *
     * @return an unmodifiable list of all profile definitions
     */
    public static List<ProfileDefinition> getAll() {
        return INSTANCE.values();
    }
}
