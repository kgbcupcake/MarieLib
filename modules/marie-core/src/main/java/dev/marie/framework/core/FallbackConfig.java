package dev.marie.framework.core;

import com.google.gson.JsonObject;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.value.ValueDefinition;
import dev.marie.framework.api.registry.ValueRegistry;
import dev.marie.framework.config.MariesLibConfigBridge;
import dev.marie.framework.config.MariesLibConfigHolder;
import dev.marie.framework.config.PresetRegistry;
import dev.marie.framework.tracking.DiminishingReturnsConfig;

/**
 * Fallback when no consuming mod has registered a {@link MarieContext}.
 * Provides library-owned scanner/debug values and safe no-op defaults for gameplay methods.
 */
@ApiStatus.Internal
enum FallbackConfig implements IMarieConfig {
    INSTANCE;

    @Override public String modId() { return MarieCore.MOD_ID; }
    @Override public float scannerConfidenceSpreadThreshold() { return MariesLibConfigHolder.get().scannerConfidenceSpreadThreshold; }
    @Override public float compositeRatioThreshold() { return MariesLibConfigHolder.get().compositeRatioThreshold; }
    @Override public boolean scannerEnableRecipeInheritance() { return MariesLibConfigHolder.get().scannerEnableRecipeInheritance; }
    @Override public boolean enableDebugLogging() { return MariesLibConfigHolder.get().enableDebugLogging; }
    @Override public double multiValueInheritanceThreshold() { return MariesLibConfigHolder.get().multiValueInheritanceThreshold; }

    @Override public long memoryWindowMinutes() { return 60L; }
    @Override public int memoryWindowCount() { return 20; }
    @Override public long streakWindowMs() { return 300_000L; }
    @Override public float streakWeight() { return 1.5f; }
    @Override public float debtThreshold() { return 5f; }
    @Override public float debtDecayRate() { return 0.01f; }
    @Override public float diminishingSteepness() { return 1.0f; }
    @Override public float diminishingMidpoint() { return 3.0f; }
    @Override public boolean debugMemoryLogging() { return false; }
    @Override public float excessThreshold() { return 0.9f; }
    @Override public float lowThreshold() { return 0.3f; }
    @Override public float criticalThreshold() { return 0.25f; }
    @Override public float criticalThresholdFor(String valueKey) { return criticalThreshold(); }
    @Override public int decayIntervalTicks() { return 20; }
    @Override public float decayRateFor(String valueKey) {
        ValueDefinition def = ValueRegistry.get(valueKey);
        return def != null ? def.getDefaultDecayRate() : 0.001f;
    }
    @Override public boolean showJoinMessage() { return false; }
    @Override public DiminishingReturnsConfig trackingMemoryConfig() { return new DiminishingReturnsConfig(60L, 1.2, 3.0, 0.2, 0.5); }
    @Override public JsonObject configExporter() { return MariesLibConfigBridge.buildExportRoot(); }
    @Override public void configImporter(JsonObject json) { MariesLibConfigBridge.applyImport(json); }
    @Override public PresetRegistry.PresetValues currentConfigPresetValues() { return PresetRegistry.PresetValues.empty(); }
    @Override public void applyPresetValues(PresetRegistry.PresetValues values) {}
    @Override public boolean trackerSystemEnabled() { return true; }
    @Override public int trackerMaxRetention() { return 90; }
    @Override public int trackerWeeklyPeriodDays() { return 7; }
    @Override public int trackerMonthlyPeriodDays() { return 30; }
    @Override public int trackerSyncIntervalTicks() { return 20; }
}
