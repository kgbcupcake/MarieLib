package dev.marie.framework.tracking;

import dev.marie.framework.api.ApiStatus;

import java.util.Locale;

/**
 * Server-side policy for tracking bar values when a player respawns after death.
 * Consumed via {@link dev.marie.framework.core.MarieLibContext#deathNutritionBehavior()}.
 */
@ApiStatus.Stable
public enum DeathNutritionBehavior {
    /** Keep bars and application memory across death (default). */
    PRESERVE("preserve"),
    /** Reset all bars to {@link DiminishingReturnsConfig#startingValueFill()} and clear application memory. */
    RESET_TO_STARTING("reset_to_starting"),
    /** Reset all bars to 50% and clear application memory (legacy behavior). */
    VANILLA_HALF("vanilla_half");

    private final String configId;

    DeathNutritionBehavior(String configId) {
        this.configId = configId;
    }

    public String configId() {
        return configId;
    }

    public static DeathNutritionBehavior fromConfigId(String raw) {
        if (raw == null || raw.isBlank()) {
            return PRESERVE;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (DeathNutritionBehavior value : values()) {
            if (value.configId.equals(normalized)) {
                return value;
            }
        }
        return PRESERVE;
    }
}
