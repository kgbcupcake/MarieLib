package dev.marie.MariesLib.core;

import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.tracking.DiminishingReturnsConfig;

/**
 * @deprecated Use {@link MarieLibSettings} for scanner/debug settings,
 * or {@link MarieLibContext#get()} for gameplay methods.
 */
@Deprecated
@ApiStatus.Internal
public interface IMarieLibConfig extends MarieLibSettings {

    @ApiStatus.Stable
    static IMarieLibConfig get() {
        if (MarieLibContext.isRegistered()) {
            return MarieLibContext.get();
        }
        return FallbackConfig.INSTANCE;
    }

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

    DiminishingReturnsConfig trackingMemoryConfig();

    JsonObject configExporter();

    void configImporter(JsonObject json);

    PresetRegistry.PresetValues currentConfigPresetValues();

    void applyPresetValues(PresetRegistry.PresetValues values);
}
