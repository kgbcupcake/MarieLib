package dev.marie.MariesLib.config;

/**
 * Stable Cloth Config / {@link LockRegistry} keys for MariesLib-owned settings.
 */
public final class MariesLibConfigKeys {

    // Modules
    public static final String ENABLE_DECAY = "modules.enableDecay";
    public static final String ENABLE_SOURCE_APPLICATION = "modules.enableSourceApplication";
    public static final String ENABLE_BLOCK_HEAVY_SOURCES = "modules.enableBlockHeavySources";
    public static final String ENABLE_BLOCK_LIGHT_SOURCE = "modules.enableBlockLightSource";
    public static final String ENABLE_EFFECTS = "modules.enableEffects";
    public static final String ENABLE_HUD = "modules.enableHUD";
    public static final String ENABLE_TOASTS = "modules.enableToasts";
    public static final String ENABLE_SOURCE_TOOLTIPS = "modules.enableSourceTooltips";
    public static final String ENABLE_TOTAL_TRACKING = "modules.enableTotalTracking";
    public static final String ENABLE_TRACKING_SCREEN = "modules.enableTrackingScreen";
    public static final String ENABLE_CRITICAL_TOASTS = "modules.enableCriticalToasts";
    public static final String ENABLE_SLEEP_BONUS = "modules.enableSleepBonus";
    public static final String ENABLE_SYNERGIES = "modules.enableSynergies";
    public static final String ENABLE_MILESTONES = "modules.enableMilestones";
    public static final String ENABLE_SEASON_HOOKS = "modules.enableSeasonHooks";
    public static final String ENABLE_ABSORPTION_MODIFIERS = "modules.enableAbsorptionModifiers";
    public static final String ENABLE_DEBUG_LOGGING = "debug.enableDebugLogging";

    // Scanner (context)
    public static final String SCANNER_CONFIDENCE_SPREAD_THRESHOLD = "scanner.confidenceSpreadThreshold";
    public static final String COMPOSITE_RATIO_THRESHOLD = "scanner.compositeRatioThreshold";
    public static final String SCANNER_ENABLE_RECIPE_INHERITANCE = "scanner.enableRecipeInheritance";
    public static final String MULTI_VALUE_INHERITANCE_THRESHOLD = "scanner.multiValueInheritanceThreshold";

    // Scanner spec multipliers
    public static final String MULT_COMMUNITY_TAG = "scanner.multipliers.communityTag";
    public static final String MULT_NAMESPACE = "scanner.multipliers.namespace";
    public static final String MULT_SUFFIX = "scanner.multipliers.suffix";
    public static final String MULT_KEYWORD = "scanner.multipliers.keyword";
    public static final String MULT_ARCHETYPE = "scanner.multipliers.archetype";
    public static final String MULT_RECIPE_INHERITANCE = "scanner.multipliers.recipeInheritance";
    public static final String MULT_NAMESPACE_PEER = "scanner.multipliers.namespacePeer";
    public static final String MULT_SECONDARY_SUFFIX = "scanner.multipliers.secondarySuffix";
    public static final String MULT_NAMESPACE_PEER_AVG = "scanner.multipliers.namespacePeerAverageWeight";

    // Memory
    public static final String MEMORY_WINDOW_MINUTES = "memory.memoryWindowMinutes";
    public static final String MEMORY_WINDOW_COUNT = "memory.memoryWindowCount";
    public static final String STREAK_WINDOW_MS = "memory.streakWindowMs";
    public static final String STREAK_WEIGHT = "memory.streakWeight";
    public static final String DEBT_THRESHOLD = "memory.debtThreshold";
    public static final String DEBT_DECAY_RATE = "memory.debtDecayRate";
    public static final String DIMINISHING_STEEPNESS = "memory.diminishingSteepness";
    public static final String DIMINISHING_MIDPOINT = "memory.diminishingMidpoint";
    public static final String NOVELTY_BONUS = "memory.noveltyBonus";
    public static final String NOVELTY_DECAY_CAP = "memory.noveltyDecayCap";
    public static final String DIMINISHING_FLOOR = "memory.diminishingFloor";
    public static final String STARTING_VALUE_FILL = "memory.startingValueFill";
    public static final String DEBUG_MEMORY_LOGGING = "memory.debugMemoryLogging";

    // Thresholds
    public static final String EXCESS_THRESHOLD = "thresholds.excessThreshold";
    public static final String LOW_THRESHOLD = "thresholds.lowThreshold";
    public static final String CRITICAL_THRESHOLD = "thresholds.criticalThreshold";
    public static final String DECAY_INTERVAL_TICKS = "thresholds.decayIntervalTicks";
    public static final String DEFAULT_DECAY_RATE = "thresholds.defaultDecayRate";

    // Effects / presets
    public static final String DEFAULT_EFFECT_DURATION_TICKS = "effects.defaultEffectDurationTicks";

    // Client
    public static final String SHOW_JOIN_MESSAGE = "client.showJoinMessage";

    private MariesLibConfigKeys() {}
}
