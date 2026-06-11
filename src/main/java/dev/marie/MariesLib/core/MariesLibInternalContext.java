package dev.marie.MariesLib.core;

import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.config.MariesLibConfigBridge;
import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigIO;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.tracking.TrackingMemoryConfig;

@ApiStatus.Internal
final class MariesLibInternalContext implements IMarieLibConfig {

    private static final MariesLibInternalContext INSTANCE = new MariesLibInternalContext();

    private MariesLibInternalContext() {}

    static MariesLibInternalContext get() {
        return INSTANCE;
    }

    @Override
    public String modId() {
        return MariesLib.MOD_ID;
    }

    @Override
    public float scannerConfidenceSpreadThreshold() {
        return MariesLibConfigHolder.get().scannerConfidenceSpreadThreshold;
    }

    @Override
    public float compositeRatioThreshold() {
        return MariesLibConfigHolder.get().compositeRatioThreshold;
    }

    @Override
    public boolean scannerEnableRecipeInheritance() {
        return MariesLibConfigHolder.get().scannerEnableRecipeInheritance;
    }

    @Override
    public boolean enableDebugLogging() {
        return MariesLibConfigHolder.get().enableDebugLogging;
    }

    @Override
    public long memoryWindowMinutes() {
        return MariesLibConfigHolder.get().memoryWindowMinutes;
    }

    @Override
    public int memoryWindowCount() {
        return MariesLibConfigHolder.get().memoryWindowCount;
    }

    @Override
    public long streakWindowMs() {
        return MariesLibConfigHolder.get().streakWindowMs;
    }

    @Override
    public float streakWeight() {
        return MariesLibConfigHolder.get().streakWeight;
    }

    @Override
    public float debtThreshold() {
        return MariesLibConfigHolder.get().debtThreshold;
    }

    @Override
    public float debtDecayRate() {
        return MariesLibConfigHolder.get().debtDecayRate;
    }

    @Override
    public float diminishingSteepness() {
        return MariesLibConfigHolder.get().diminishingSteepness;
    }

    @Override
    public float diminishingMidpoint() {
        return MariesLibConfigHolder.get().diminishingMidpoint;
    }

    @Override
    public boolean debugMemoryLogging() {
        return MariesLibConfigHolder.get().debugMemoryLogging;
    }

    @Override
    public float excessThreshold() {
        return MariesLibConfigHolder.get().excessThreshold;
    }

    @Override
    public float lowThreshold() {
        return MariesLibConfigHolder.get().lowThreshold;
    }

    @Override
    public float criticalThreshold() {
        return MariesLibConfigHolder.get().criticalThreshold;
    }

    @Override
    public float criticalThresholdFor(String valueKey) {
        ValueDefinition def = ValueRegistry.get(valueKey);
        return def != null ? def.getCriticalThreshold() : criticalThreshold();
    }

    @Override
    public int decayIntervalTicks() {
        return MariesLibConfigHolder.get().decayIntervalTicks;
    }

    @Override
    public boolean showJoinMessage() {
        return MariesLibConfigHolder.get().showJoinMessage;
    }

    @Override
    public double multiValueInheritanceThreshold() {
        return MariesLibConfigHolder.get().multiValueInheritanceThreshold;
    }

    @Override
    public TrackingMemoryConfig trackingMemoryConfig() {
        return MariesLibConfigHolder.get().toTrackingMemoryConfig();
    }

    @Override
    public JsonObject configExporter() {
        return MariesLibConfigBridge.buildExportRoot();
    }

    @Override
    public void configImporter(JsonObject json) {
        MariesLibConfigBridge.applyImport(json);
    }

    @Override
    public PresetRegistry.PresetValues currentConfigPresetValues() {
        return MariesLibConfigHolder.get().toPresetValues();
    }

    @Override
    public void applyPresetValues(PresetRegistry.PresetValues values) {
        MariesLibConfigHolder.get().applyPresetValues(values);
        MariesLibConfigIO.save();
    }
}
