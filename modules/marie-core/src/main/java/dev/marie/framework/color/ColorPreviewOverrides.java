package dev.marie.framework.color;

import dev.marie.framework.api.ApiStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transient, non-persisted ARGB overrides for {@link ColorKey}s, consulted by
 * {@link MarieColors#resolveColor(ColorKey)} ahead of
 * {@link ColorRegistry}. Lets config UI (e.g. a hex row widget) push a color change so it's
 * visible everywhere {@code resolveColor} is read (HUD, panels, ...) immediately as the user
 * types, without writing to {@link ColorRegistry} until an explicit save.
 */
@ApiStatus.Stable
public final class ColorPreviewOverrides {

    private static final Map<ColorKey, Integer> OVERRIDES = new ConcurrentHashMap<>();

    private ColorPreviewOverrides() {}

    /**
     * Sets a transient ARGB override for a color key, or clears it when {@code argb} is null.
     */
    @ApiStatus.Stable
    public static void setOverride(ColorKey key, Integer argb) {
        if (argb == null) {
            OVERRIDES.remove(key);
        } else {
            OVERRIDES.put(key, argb);
        }
    }

    /** Returns the transient override for a color key, or {@code null} if none is set. */
    @ApiStatus.Stable
    public static Integer getOverride(ColorKey key) {
        return OVERRIDES.get(key);
    }

    /** Clears all transient color overrides. */
    @ApiStatus.Stable
    public static void clearOverrides() {
        OVERRIDES.clear();
    }
}
