package dev.marie.framework.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.config.validation.ConfigValidatorRegistry;
import dev.marie.framework.api.registry.AbsorptionModifierRegistry;
import dev.marie.framework.api.registry.ProfileRegistry;
import dev.marie.framework.api.registry.MilestoneRegistry;
import dev.marie.framework.api.registry.ReportProviderRegistry;
import dev.marie.framework.api.registry.SeasonHookRegistry;
import dev.marie.framework.api.registry.SleepBonusEvaluatorRegistry;
import dev.marie.framework.api.registry.SourcePropertySignalRegistry;
import dev.marie.framework.api.registry.SynergyRegistry;
import dev.marie.framework.runtime.TriggerHandlerRegistry;

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
     * Called at the start of each {@link dev.marie.framework.data.MarieDataLoader} apply pass.
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
