package dev.marie.MariesLib.core;

import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.tracking.TrackingMemoryConfig;

@ApiStatus.Internal
public interface IMarieLibConfig {

    static IMarieLibConfig get() {
        if (MarieLibContext.isRegistered()) {
            return MarieLibContext.get();
        }
        return MariesLibInternalContext.get();
    }

    String modId();

    float scannerConfidenceSpreadThreshold();

    float compositeRatioThreshold();

    boolean scannerEnableRecipeInheritance();

    boolean enableDebugLogging();

    long memoryWindowMinutes();

    int memoryWindowCount();

    long streakWindowMs();

    float streakWeight();

    float debtThreshold();

    float debtDecayRate();

    float diminishingSteepness();

    float diminishingMidpoint();

    boolean debugMemoryLogging();

    float excessThreshold();

    float lowThreshold();

    float criticalThreshold();

    float criticalThresholdFor(String valueKey);

    int decayIntervalTicks();

    boolean showJoinMessage();

    double multiValueInheritanceThreshold();

    TrackingMemoryConfig trackingMemoryConfig();

    JsonObject configExporter();

    void configImporter(JsonObject json);

    PresetRegistry.PresetValues currentConfigPresetValues();

    void applyPresetValues(PresetRegistry.PresetValues values);
}
