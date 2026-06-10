package dev.marie.MariesLib.client;

import dev.marie.MariesLib.color.ColorRegistry;
import dev.marie.MariesLib.core.MarieLibContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared ARGB palette for value keys. Uses registry index for dynamic lookup.
 */
public final class MarieValueColors {

    /** Transient UI overrides (e.g. config preview / color pickers); never persisted to disk. */
    private static final Map<String, Integer> OVERRIDES = new ConcurrentHashMap<>();

    private static final int[] PALETTE = {
            0xFF55FF55,  // green
            0xFF4DD9D9,  // cyan
            0xFFFFD65C,  // gold
            0xFFA95FFF,  // purple
            0xFFFFFFFF,  // white
            0xFFFF9955,  // orange
            0xFF55AAFF   // blue
    };

    private MarieValueColors() {}

    /**
     * Sets a transient ARGB override for a value key, or clears it when {@code argb} is null.
     */
    public static void setOverride(String key, Integer argb) {
        if (argb == null) {
            OVERRIDES.remove(key);
        } else {
            OVERRIDES.put(key, argb);
        }
    }

    /** Clears all transient color overrides. */
    public static void clearOverrides() {
        OVERRIDES.clear();
    }

    /** Default palette entry for a key (no {@link ColorRegistry} or transient override). */
    public static int paletteOnlyArgb(String key) {
        List<String> keys = MarieLibContext.get().valueKeys();
        int idx = keys.indexOf(key);
        if (idx < 0) {
            return PALETTE[0];
        }
        return PALETTE[idx % PALETTE.length];
    }

    /**
     * ARGB accent color: transient override, then {@link ColorRegistry}, then the built-in palette.
     */
    public static int baseColorArgb(String key) {
        Integer override = OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        return ColorRegistry.getArgb(key).orElseGet(() -> paletteOnlyArgb(key));
    }
}
