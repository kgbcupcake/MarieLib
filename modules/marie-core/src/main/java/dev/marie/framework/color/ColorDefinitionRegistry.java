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
     * Temporarily reopens the registry for re-registration during
     * {@code MarieContext.reloadBroadcastHook()}. Must be paired with a subsequent
     * {@link #freezeInternal()} in a {@code finally} block.
     */
    @ApiStatus.Internal
    public static void unfreezeInternal() {
        INSTANCE.unfreeze();
    }

    /**
     * Registers a color definition. Re-registering an already-registered key replaces the
     * existing definition rather than throwing — colors are expected to be re-registered on
     * every reload (see {@code MarieContext.reloadBroadcastHook()}).
     *
     * @param definition the color to register
     * @throws IllegalStateException    if the registry is frozen
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void register(ColorDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        INSTANCE.upsert(definition.getKey(), definition);
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
