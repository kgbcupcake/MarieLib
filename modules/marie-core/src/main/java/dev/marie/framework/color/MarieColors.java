package dev.marie.framework.color;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marieapi.MarieAPIState;
import dev.marie.framework.core.MarieCore;
import net.minecraft.resources.ResourceLocation;

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
     * <p>Must be called during mod initialization or datapack reload. Re-registering an
     * already-registered key is safe and replaces the existing definition rather than throwing —
     * every {@code /reload} wipes {@link ColorDefinitionRegistry}, so consumers that want their
     * colors to survive a reload should call this again from
     * {@code MarieContext.reloadBroadcastHook()} (see
     * {@link dev.marie.framework.handler.ReloadGuardListener#reloadAndBroadcast}); nothing else
     * re-invokes registration code automatically.</p>
     *
     * @param definition the color definition to register
     * @throws IllegalStateException    if called outside the registration window
     * @throws IllegalArgumentException if {@code definition} is null
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
     * Registers a linked background/text {@link ColorKey} pair under {@code modId}, so panels
     * needing both no longer hand-register two separate {@link ColorDefinition}s. Builds
     * {@code panel.<id>} and {@code text.<id>} keys and registers each with its own default ARGB.
     *
     * <p>Must be called during mod initialization or datapack reload, same as {@link #registerColor}.</p>
     *
     * @param modId               the mod namespace for both keys
     * @param id                  the shared id suffix for both keys
     * @param backgroundDefaultArgb default ARGB for the background key
     * @param textDefaultArgb       default ARGB for the text key
     * @return the registered {@link ColorKeyPair}
     * @throws IllegalStateException    if called outside the registration window
     * @throws IllegalArgumentException if either resulting color key already exists
     */
    @ApiStatus.Stable
    public static ColorKeyPair registerColorPair(String modId, String id, int backgroundDefaultArgb, int textDefaultArgb) {
        MarieAPIState.assertRegistrationAllowed("registerColorPair");
        ColorKey backgroundKey = ColorKey.of(ResourceLocation.fromNamespaceAndPath(modId, "panel." + id));
        ColorKey textKey = ColorKey.of(ResourceLocation.fromNamespaceAndPath(modId, "text." + id));
        ColorDefinitionRegistry.register(ColorDefinition.of(backgroundKey, backgroundDefaultArgb));
        ColorDefinitionRegistry.register(ColorDefinition.of(textKey, textDefaultArgb));
        return new ColorKeyPair(backgroundKey, textKey);
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
        Integer preview = ColorPreviewOverrides.getOverride(key);
        if (preview != null) {
            return preview;
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
