package dev.marie.framework.api.marie;

import dev.marie.framework.api.marieapi.MarieAPI;

import dev.marie.framework.api.ApiStatus;

/**
 * Hook interface for integrating with Serene Seasons (or similar season mods),
 * allowing value decay rates and source availability to vary per season.
 *
 * <p>Implement this interface and register it via
 * {@link MarieAPI#registerSeasonHook(MarieSeasonHook)} to provide
 * seasonal modifiers to the value system.</p>
 */
@ApiStatus.Experimental
public interface MarieSeasonHook {

    /**
     * Represents the four canonical seasons used by the hook system.
     */
    enum Season {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER
    }

    /**
     * Returns a decay rate modifier for the specified value during the given season.
     *
     * <p>A value of {@code 1.0} means no change, values greater than {@code 1.0}
     * accelerate decay, and values less than {@code 1.0} slow it down.</p>
     *
     * @param valueKey the internal key identifying the value (e.g. "value_a")
     * @param season      the current season to calculate the modifier for
     * @return the multiplicative decay modifier; must be non-negative
     */
    float getSeasonalDecayModifier(String valueKey, Season season);

    /**
     * Returns a value absorption modifier for the specified value during
     * the given season, affecting how much value is gained from a source.
     *
     * <p>A value of {@code 1.0} means standard absorption. Values greater than
     * {@code 1.0} increase gains (e.g. seasonal sources in season), and values
     * less than {@code 1.0} reduce them.</p>
     *
     * @param valueKey the internal key identifying the value
     * @param season      the current season to calculate the modifier for
     * @return the multiplicative absorption modifier; must be non-negative
     */
    float getSeasonalAbsorptionModifier(String valueKey, Season season);
}
