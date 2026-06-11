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
    public static boolean enableBlockLightSource = false;
    public static boolean enableEffects = true;
    public static boolean enableHUD = true;
    public static boolean enableToasts = true;
    public static boolean enableSourceTooltips = true;
    public static boolean enableTotalTracking = true;
    public static boolean enableTrackingScreen = true;
    public static boolean enableCriticalToasts = true;
    public static boolean enableSleepBonus = true;
    public static boolean enableSynergies = true;
    public static boolean enableMilestones = true;
    public static boolean enableSeasonHooks = true;
    public static boolean enableAbsorptionModifiers = true;
    public static boolean enableDebugLogging = false;

    private static volatile boolean initialized;

    private ModuleCache() {}

    public static boolean isInitialized() {
        return initialized;
    }

    public static void refresh() {
        MariesLibConfigHolder h = MariesLibConfigHolder.get();
        enableDecay = h.enableDecay;
        enableSourceApplication = h.enableSourceApplication;
        enableBlockHeavySources = h.enableBlockHeavySources;
        enableBlockLightSource = h.enableBlockLightSource;
        enableEffects = h.enableEffects;
        enableHUD = h.enableHUD;
        enableToasts = h.enableToasts;
        enableSourceTooltips = h.enableSourceTooltips;
        enableTotalTracking = h.enableTotalTracking;
        enableTrackingScreen = h.enableTrackingScreen;
        enableCriticalToasts = h.enableCriticalToasts;
        enableSleepBonus = h.enableSleepBonus;
        enableSynergies = h.enableSynergies;
        enableMilestones = h.enableMilestones;
        enableSeasonHooks = h.enableSeasonHooks;
        enableAbsorptionModifiers = h.enableAbsorptionModifiers;
        enableDebugLogging = h.enableDebugLogging;
        initialized = true;
    }
}
