package dev.marie.framework.color;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.registry.AbstractRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Internal storage for color definitions registered via the public API.
 */
@ApiStatus.Internal
public final class ColorDefinitionRegistry {

    private static final class Core extends AbstractRegistry<ColorKey, ColorDefinition> {
        Core() {
            super("ColorDefinitionRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private ColorDefinitionRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        INSTANCE.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        INSTANCE.reset();
    }

    /**
     * Registers a color definition.
     *
     * @param definition the color to register
     * @throws IllegalStateException    if the registry is frozen or a color with the same key already exists
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void register(ColorDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        INSTANCE.register(definition.getKey(), definition);
    }

    /**
     * Returns a registered color by key, or {@code null} if not found.
     *
     * @param key the color identifier
     * @return the color definition, or {@code null}
     */
    @Nullable
    public static ColorDefinition get(ColorKey key) {
        return INSTANCE.get(key);
    }

    /**
     * Returns all registered color definitions.
     *
     * @return an unmodifiable list of all color definitions
     */
    public static List<ColorDefinition> getAll() {
        return INSTANCE.values();
    }
}
