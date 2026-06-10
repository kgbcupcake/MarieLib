package dev.marie.MariesLib.config;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * Cached module toggle values for hot gameplay paths (render/tick/effect loops).
 * Refresh values after config load/reload/save.
 */
@ApiStatus.Internal
public final class ModuleCache {

    public static boolean enableDecay = true;
    public static boolean enableSourceApplication = true;
    public static boolean enableBlockHeavySources = false;
    /** Fallback / config value for heavy-source property threshold (see {@link ModCompatRegistry#getHeavySourceThreshold()}). */
    public static int heavySourcePropertyThreshold = 6;
    public static boolean enableBlockLightSource = false;
    public static boolean enableEffects = true;
    public static boolean enableHUD = true;
    public static boolean enableToasts = true;
    public static boolean enableSourceTooltips = true;
    public static boolean enableTotalTracking = true;
    public static boolean enableTrackingScreen = true;
    public static boolean enableCriticalToasts = true;
    public static boolean enableSleepBonus = true;
    public static boolean enableRawSourcePenalty = true;
    public static boolean enableGutHealth = true;
    // public static boolean enableStamina = true; // STAMINA_SHELVED
    public static boolean enableStamina = false;
    public static boolean enablePSStaminaUsage = true;
    public static boolean enablePSPenaltyDecay = true;
    public static boolean enablePSExhaustionDuration = true;
    public static boolean enableSOLDiversityHealth = false;
    public static boolean enableSOLDiversityPenalty = true;
    public static boolean enableLSOThermalResistance = true;
    public static boolean enableLSOBrokenHeartResilience = true;
    public static boolean enableLSOThirstSaturation = true;
    public static boolean enableSynergies = true;
    public static boolean enableMilestones = true;
    public static boolean enableSeasonHooks = true;
    public static boolean enableAbsorptionModifiers = true;
    public static boolean enableDebugLogging = false;

    private ModuleCache() {}

    public static void refresh() {
        // Populated by consuming mod.
    }
}
