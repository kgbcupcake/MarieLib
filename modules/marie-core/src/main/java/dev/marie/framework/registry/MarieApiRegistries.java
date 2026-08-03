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
import dev.marie.framework.color.ColorDefinitionRegistry;
import dev.marie.framework.runtime.TriggerHandlerRegistry;
import dev.marie.framework.tracking.tracker.registry.TrackerRegistry;

/**
 * Coordinates freeze/reset for API definition registries around bootstrap and datapack reload.
 */
@ApiStatus.Internal
public final class MarieApiRegistries {

    private static boolean datapackApplyCompletedOnce;

    private MarieApiRegistries() {}

    /**
     * Freezes value-tracking-specific list registries that only receive mod-constructor
     * registrations (no datapack pass). Domain-agnostic registries (e.g.
     * {@code BlockHoverProviderRegistry}) are frozen separately via
     * {@link dev.marie.framework.core.MarieBootstrap#attachFrameworkServices}.
     */
    public static void freezeValueTrackingOnlyRegistriesAfterCommonSetup() {
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
     *
     * <p>{@link TrackerRegistry} and {@link ColorDefinitionRegistry} have no datapack-driven
     * repopulation path (no {@code registerTracker}/{@code registerColor} slot in
     * {@link dev.marie.framework.data.MarieDataLoader.Callbacks}), so entries registered purely
     * via Java at mod init (e.g. {@code MarieAPI.registerTracker}, {@code MarieColors.registerColor})
     * are wiped here on every reload after the first and are not restored automatically — nothing
     * re-invokes the consuming mod's registration code on its own. Consuming mods should re-invoke
     * their registration code from {@code MarieContext.reloadBroadcastHook()}
     * ({@link dev.marie.framework.handler.ReloadGuardListener#reloadAndBroadcast}), which fires
     * after this reset/refreeze pass completes for every reload; re-registration is safe to repeat
     * since these two registries upsert on duplicate keys. {@code ProfileRegistry}/
     * {@code MilestoneRegistry}/{@code SynergyRegistry} have a working datapack callback loop in
     * {@code MarieDataLoader} that refills datapack-sourced entries, but any of *their* entries
     * registered purely via Java (not from a datapack file) would have this exact same gap and
     * should use the same hook.</p>
     */
    public static void onDatapackApplyBegin() {
        if (datapackApplyCompletedOnce) {
            ProfileRegistry.resetInternal();
            MilestoneRegistry.resetInternal();
            SynergyRegistry.resetInternal();
            TrackerRegistry.resetInternal();
            ColorDefinitionRegistry.resetInternal();
        }
    }

    /**
     * Called at the end of each datapack apply pass, before the reload scope closes.
     */
    public static void onDatapackApplyEnd() {
        ProfileRegistry.freezeInternal();
        MilestoneRegistry.freezeInternal();
        SynergyRegistry.freezeInternal();
        TrackerRegistry.freezeInternal();
        ColorDefinitionRegistry.freezeInternal();
        datapackApplyCompletedOnce = true;
    }
}
