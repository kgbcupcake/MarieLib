package dev.marie.framework.color;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marieapi.MarieAPIState;
import dev.marie.framework.core.MarieCore;

/**
 * Public facade for the MarieLib color subsystem: registering default color definitions and
 * resolving their effective, currently-in-effect ARGB value. Self-contained and separate from
 * {@link dev.marie.framework.api.marieapi.MarieAPI} — colors have their own definition/override
 * split ({@link ColorDefinitionRegistry} for defaults, {@link ColorRegistry} for user/datapack
 * overrides) rather than routing through the general API surface.
 */
@ApiStatus.Stable
public final class MarieColors {

    /** Loud fallback for an unregistered {@link ColorKey} — a real bug, never silently black/white. */
    private static final int UNREGISTERED_ARGB = 0xFFFF00FF;

    private MarieColors() {}

    /**
     * Registers a color definition, providing its default ARGB value for {@link #resolveColor(ColorKey)}
     * to fall back on when no user/datapack override is present.
     *
     * <p>Must be called during mod initialization or datapack reload.</p>
     *
     * @param definition the color definition to register
     * @throws IllegalStateException    if called outside the registration window
     * @throws IllegalArgumentException if {@code definition} is null or a color with the same
     *                                   key already exists
     */
    @ApiStatus.Stable
    public static void registerColor(ColorDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerColor");
        if (definition == null) {
            throw new IllegalArgumentException("registerColor: definition must not be null");
        }
        ColorDefinitionRegistry.register(definition);
    }

    /**
     * Resolves the effective ARGB color for a key: a user/datapack override from
     * {@link ColorRegistry} if one exists, otherwise the registered {@link ColorDefinition}'s
     * default. If the key was never registered at all, logs a warning and returns a hardcoded
     * magenta error color rather than silently returning black or white.
     *
     * @param key the color identity to resolve
     * @return the effective packed ARGB color
     * @throws IllegalArgumentException if {@code key} is null
     */
    @ApiStatus.Stable
    public static int resolveColor(ColorKey key) {
        if (key == null) {
            throw new IllegalArgumentException("resolveColor: key must not be null");
        }
        return ColorRegistry.getArgb(colorRegistryKey(key)).orElseGet(() -> {
            ColorDefinition definition = ColorDefinitionRegistry.get(key);
            if (definition != null) {
                return definition.getDefaultArgb();
            }
            MarieCore.LOGGER.warn("[MarieColors] resolveColor: no ColorDefinition registered for key '{}', " +
                    "returning error color", key.id());
            return UNREGISTERED_ARGB;
        });
    }

    /** Canonical {@link ColorKey}↔{@link ColorRegistry} String-key mapping. */
    private static String colorRegistryKey(ColorKey key) {
        return key.id().toString();
    }
}
