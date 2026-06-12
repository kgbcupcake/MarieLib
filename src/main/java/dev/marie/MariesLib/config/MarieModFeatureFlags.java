package dev.marie.MariesLib.config;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * Immutable snapshot of feature flags supplied by the consuming Marie mod.
 * MariesLib does not define defaults for these — consuming mods must provide values.
 */
@ApiStatus.Stable
public record MarieModFeatureFlags(
        boolean enableDecay,
        boolean enableSourceApplication,
        boolean enableBlockHeavySources,
        boolean enableBlockLightSource,
        boolean enableEffects,
        boolean enableHUD,
        boolean enableToasts,
        boolean enableSourceTooltips,
        boolean enableTotalTracking,
        boolean enableTrackingScreen,
        boolean enableCriticalToasts,
        boolean enableSleepBonus,
        boolean enableSynergies,
        boolean enableMilestones,
        boolean enableSeasonHooks,
        boolean enableAbsorptionModifiers,
        boolean enableDebugLogging
) {
    /**
     * Conservative defaults: all pipelines disabled.
     * Used when no consuming mod has registered.
     */
    public static MarieModFeatureFlags disabled() {
        return new MarieModFeatureFlags(
                false, false, false, false, false,
                false, false, false, false, false,
                false, false, false, false, false,
                false, false
        );
    }
}
