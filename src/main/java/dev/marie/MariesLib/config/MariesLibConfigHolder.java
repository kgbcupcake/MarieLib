package dev.marie.MariesLib.config;

import dev.marie.MariesLib.tracking.TrackingMemoryConfig;

/**
 * Mutable source of truth for all MariesLib-owned scalar configuration.
 */
public final class MariesLibConfigHolder {

    private static final MariesLibConfigHolder INSTANCE = new MariesLibConfigHolder();

    // ModuleCache mirrors
    public boolean enableDecay = true;
    public boolean enableSourceApplication = true;
    public boolean enableBlockHeavySources = false;
    public boolean enableBlockLightSource = false;
    public boolean enableEffects = true;
    public boolean enableHUD = true;
    public boolean enableToasts = true;
    public boolean enableSourceTooltips = true;
    public boolean enableTotalTracking = true;
    public boolean enableTrackingScreen = true;
    public boolean enableCriticalToasts = true;
    public boolean enableSleepBonus = true;
    public boolean enableSynergies = true;
    public boolean enableMilestones = true;
    public boolean enableSeasonHooks = true;
    public boolean enableAbsorptionModifiers = true;
    public boolean enableDebugLogging = false;

    // Scanner context
    public float scannerConfidenceSpreadThreshold = 0f;
    public float compositeRatioThreshold = 0f;
    public boolean scannerEnableRecipeInheritance = false;
    public double multiValueInheritanceThreshold = 0.20;

    // Scanner spec multipliers
    public float multCommunityTag = 5.0f;
    public float multNamespace = 4.0f;
    public float multSuffix = 3.0f;
    public float multKeyword = 2.0f;
    public float multArchetype = 2.0f;
    public float multRecipeInheritance = 1.0f;
    public float multNamespacePeer = 0.5f;
    public float multSecondarySuffix = 0.5f;
    public float multNamespacePeerAverageWeight = 0.5f;

    // Memory
    public long memoryWindowMinutes = 60L;
    public int memoryWindowCount = 20;
    public long streakWindowMs = 300_000L;
    public float streakWeight = 1.5f;
    public float debtThreshold = 5f;
    public float debtDecayRate = 0.01f;
    public float diminishingSteepness = 1.0f;
    public float diminishingMidpoint = 3.0f;
    public double noveltyBonus = 1.2;
    public double noveltyDecayCap = 3.0;
    public double diminishingFloor = 0.2;
    public double startingValueFill = 0.5;
    public boolean debugMemoryLogging = false;

    // Thresholds
    public float excessThreshold = 0.9f;
    public float lowThreshold = 0.3f;
    public float criticalThreshold = 0.25f;
    public int decayIntervalTicks = 20;
    public float defaultDecayRate = 0f;

    // Effects / presets
    public int defaultEffectDurationTicks = 140;

    // Client
    public boolean showJoinMessage = false;

    private MariesLibConfigHolder() {}

    public static MariesLibConfigHolder get() {
        return INSTANCE;
    }

    public TrackingMemoryConfig toTrackingMemoryConfig() {
        return new TrackingMemoryConfig(
                memoryWindowMinutes,
                noveltyBonus,
                noveltyDecayCap,
                diminishingFloor,
                startingValueFill);
    }

    public PresetRegistry.PresetValues toPresetValues() {
        return new PresetRegistry.PresetValues(
                defaultDecayRate,
                criticalThreshold,
                lowThreshold,
                excessThreshold,
                defaultEffectDurationTicks,
                enableDecay,
                enableEffects);
    }

    public void applyPresetValues(PresetRegistry.PresetValues v) {
        defaultDecayRate = (float) v.decayRate();
        criticalThreshold = (float) v.criticalThreshold();
        lowThreshold = (float) v.lowThreshold();
        excessThreshold = (float) v.excessThreshold();
        defaultEffectDurationTicks = v.defaultEffectDurationTicks();
        enableDecay = v.enableDecay();
        enableEffects = v.enableEffects();
    }

    /** Copies current {@link ScannerSpecRegistry} scalar values into this holder. */
    public void loadScannerScalarsFromRegistry() {
        var spec = dev.marie.MariesLib.scanner.ScannerSpecRegistry.get();
        var mult = spec.multipliers();
        multCommunityTag = mult.communityTag();
        multNamespace = mult.namespace();
        multSuffix = mult.suffix();
        multKeyword = mult.keyword();
        multArchetype = mult.archetype();
        multRecipeInheritance = mult.recipeInheritance();
        multNamespacePeer = mult.namespacePeer();
        multSecondarySuffix = mult.secondarySuffix();
        multNamespacePeerAverageWeight = mult.namespacePeerAverageWeight();
    }
}
