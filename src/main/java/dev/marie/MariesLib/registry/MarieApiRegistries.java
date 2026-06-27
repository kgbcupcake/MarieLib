package dev.marie.MariesLib.registry;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.validation.ConfigValidatorRegistry;
import dev.marie.MariesLib.api.registry.AbsorptionModifierRegistry;
import dev.marie.MariesLib.api.registry.ProfileRegistry;
import dev.marie.MariesLib.api.registry.MilestoneRegistry;
import dev.marie.MariesLib.api.registry.ReportProviderRegistry;
import dev.marie.MariesLib.api.registry.SeasonHookRegistry;
import dev.marie.MariesLib.api.registry.SleepBonusEvaluatorRegistry;
import dev.marie.MariesLib.api.registry.SourcePropertySignalRegistry;
import dev.marie.MariesLib.api.registry.SynergyRegistry;
import dev.marie.MariesLib.runtime.TriggerHandlerRegistry;

/**
 * Coordinates freeze/reset for API definition registries around bootstrap and datapack reload.
 */
@ApiStatus.Internal
public final class MarieApiRegistries {

    private static boolean datapackApplyCompletedOnce;

    private MarieApiRegistries() {}

    /**
     * Freezes list registries that only receive mod-constructor registrations (no datapack pass).
     */
    public static void freezeModOnlyRegistriesAfterCommonSetup() {
        AbsorptionModifierRegistry.freezeInternal();
        SeasonHookRegistry.freezeInternal();
        ReportProviderRegistry.freezeInternal();
        SourcePropertySignalRegistry.freezeInternal();
        SleepBonusEvaluatorRegistry.freezeInternal();
        TriggerHandlerRegistry.freezeInternal();
        ConfigValidatorRegistry.freezeInternal();
    }

    /**
     * Called at the start of each {@link dev.marie.MariesLib.data.MarieDataLoader} apply pass.
     * On the first pass, mod-constructor entries are preserved; on later passes, datapack-backed
     * registries are cleared before JSON is re-applied.
     */
    public static void onDatapackApplyBegin() {
        if (datapackApplyCompletedOnce) {
            ProfileRegistry.resetInternal();
            MilestoneRegistry.resetInternal();
            SynergyRegistry.resetInternal();
        }
    }

    /**
     * Called at the end of each datapack apply pass, before the reload scope closes.
     */
    public static void onDatapackApplyEnd() {
        ProfileRegistry.freezeInternal();
        MilestoneRegistry.freezeInternal();
        SynergyRegistry.freezeInternal();
        datapackApplyCompletedOnce = true;
    }
}
