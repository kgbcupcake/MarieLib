package dev.marie.framework.tracking.tracker.definition;

import dev.marie.framework.api.ApiStatus;

import java.util.Locale;

/**
 * Boundary policy for a {@link TrackerDefinition}'s accumulation period.
 */
@ApiStatus.Stable
public enum TrackerPeriod {
    DAILY("daily"),
    /**
     * A fixed number of in-game days (see IMarieConfig#trackerWeeklyPeriodDays()).
     */
    WEEKLY("weekly"),
    /**
     * A fixed number of in-game days (see IMarieConfig#trackerMonthlyPeriodDays()) — NOT a real-world calendar month.
     */
    MONTHLY("monthly"),
    REAL_TIME("real_time"),
    SESSION("session"),
    CUSTOM("custom");

    private final String configId;

    TrackerPeriod(String configId) {
        this.configId = configId;
    }

    public String configId() {
        return configId;
    }

    public static TrackerPeriod fromConfigId(String raw) {
        if (raw == null || raw.isBlank()) {
            return DAILY;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (TrackerPeriod value : values()) {
            if (value.configId.equals(normalized)) {
                return value;
            }
        }
        return DAILY;
    }
}
