package dev.marie.MariesLib.api;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPIState;
import dev.marie.MariesLib.api.MarieAPIVersion;
import dev.marie.MariesLib.api.impl.EmptyApplicationHistoryView;
import dev.marie.MariesLib.compat.CompatDefinition;
import dev.marie.MariesLib.api.registry.AbsorptionModifierRegistry;
import dev.marie.MariesLib.api.registry.ProfileRegistry;
import dev.marie.MariesLib.api.registry.MilestoneRegistry;
import dev.marie.MariesLib.api.registry.ReportProviderRegistry;
import dev.marie.MariesLib.api.registry.SeasonHookRegistry;
import dev.marie.MariesLib.api.registry.SleepBonusEvaluatorRegistry;
import dev.marie.MariesLib.api.registry.SourcePropertySignalRegistry;
import dev.marie.MariesLib.api.registry.SynergyRegistry;
import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MarieLibDataProvider;
import dev.marie.MariesLib.core.MarieLibRegistrationDelegate;
import dev.marie.MariesLib.handler.SourceApplicationPipeline;
import dev.marie.MariesLib.runtime.SourceTriggerRegistry;
import dev.marie.MariesLib.runtime.TriggerHandlerRegistry;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static entry point for the MarieLib public API.
 *
 * <p>All interaction with the MarieLib value system from external mods
 * should go through this class. Methods provide read access to player value
 * state and write access to register custom values, effects, compatibilities,
 * and extension hooks.</p>
 *
 * <p>This class is not instantiable. All methods are static.</p>
 */
@ApiStatus.Stable
public final class MarieAPI {

    private static ResourceLocation apiModifierSource() {
        String modId = IMarieLibConfig.get().modId();
        return ResourceLocation.fromNamespaceAndPath(modId, "api");
    }

    private MarieAPI() {}

    // ───────────────────────────────────────────────────────────────
    // Player State Queries
    // ───────────────────────────────────────────────────────────────

    /**
     * Returns the aggregate level for the given player based on their
     * current value levels and recent source consumption.
     *
     * @param player the player to query
     * @return the player's current aggregate level
     * @throws IllegalStateException if the value system is not initialized
     */
    public static float getAggregateLevel(Player player) {
        if (player == null) {
            return 0f;
        }
        MarieLibDataProvider provider = MarieLibContext.get().dataProvider();
        if (provider == null) {
            return 0f;
        }
        return provider.getAggregateLevel(player);
    }

    /**
     * Returns the current level of a specific value for the given player.
     *
     * @param player      the player to query
     * @param valueKey the internal key of the value (e.g. "value_a")
     * @return the value level as a normalized float (0.0 to 1.0),
     *         or {@code -1.0f} if the value key is not recognized
     */
    public static float getValueLevel(Player player, String valueKey) {
        if (player == null) {
            return -1.0f;
        }
        MarieLibDataProvider provider = MarieLibContext.get().dataProvider();
        if (provider == null) {
            return -1.0f;
        }
        return provider.getValueLevel(player, valueKey);
    }

    /**
     * Returns a read-only view of the player's source consumption memory,
     * exposing recent applying history and variety information.
     *
     * @param player the player to query
     * @return an {@link ApplicationHistoryView} for the given player
     * @throws IllegalStateException if the value system is not initialized
     */
    public static ApplicationHistoryView getApplicationHistory(Player player) {
        if (player == null) {
            return EmptyApplicationHistoryView.INSTANCE;
        }
        MarieLibDataProvider provider = MarieLibContext.get().dataProvider();
        if (provider == null) {
            return EmptyApplicationHistoryView.INSTANCE;
        }
        return provider.getApplicationHistoryView(player);
    }
    /**
     * Alias for {@link #getAggregateLevel(Player)}.
     *
     * @param player the player to query
     * @return the player's current aggregate level
     */
    @ApiStatus.Stable
    public static float getTotalCount(Player player) {
        return getAggregateLevel(player);
    }

    /**
     * Returns an aggregated tracking snapshot for the given player including
     * total, all registered value levels, and source memory state.
     *
     * @param player the player to query
     * @return aggregated player tracking state snapshot
     */
    @ApiStatus.Stable
    public static MariePlayerData getTrackingData(Player player) {
        Map<String, Float> values = new LinkedHashMap<>();
        MarieLibRegistrationDelegate delegate = MarieLibContext.get().registrationDelegate();
        if (delegate != null) {
            for (String valueKey : delegate.getValueKeys()) {
                values.put(valueKey, getValueLevel(player, valueKey));
            }
        }
        return new MariePlayerData(
                getAggregateLevel(player),
                Collections.unmodifiableMap(values),
                getApplicationHistory(player)
        );
    }

    /**
     * Applies a direct value delta by posting a {@link ValueModifierEvent}
     * and then applying the final event amount if the event is not cancelled.
     *
     * @param player      the player to modify
     * @param valueKey the value key to modify
     * @param delta       the value delta to apply
     */
    @ApiStatus.Stable
    public static void modifyValue(Player player, String valueKey, float delta) {
        MarieLibDataProvider provider = MarieLibContext.get().dataProvider();
        if (provider == null) {
            return;
        }
        ValueModifierEvent modifierEvent = new ValueModifierEvent(player, apiModifierSource(), valueKey, delta);
        NeoForge.EVENT_BUS.post(modifierEvent);
        if (modifierEvent.isCanceled()) {
            return;
        }
        provider.modifyValue(player, valueKey, modifierEvent.getAmount());
    }

    @ApiStatus.Stable
    public static String getVersion() {
        return MarieAPIVersion.VERSION;
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Values & Sources
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a custom value with the MarieLib system. The value will
     * participate in all standard mechanics (decay, thresholds, HUD rendering).
     *
     * <p>Must be called during mod initialization (before the server starts).</p>
     *
     * @param definition the value definition to register
     * @throws IllegalStateException    if called after initialization is complete
     * @throws IllegalArgumentException if a value with the same id already exists
     */
    public static void registerValue(ValueDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerValue");
        MarieLibRegistrationDelegate delegate = MarieLibContext.get().registrationDelegate();
        if (delegate == null) {
            throw new IllegalStateException("MarieLib registration delegate not configured");
        }
        if (delegate.getValueKeys().contains(definition.getId())) {
            throw new IllegalArgumentException("Value already registered: " + definition.getId());
        }
        delegate.registerValue(definition);
        ValueRegistry.register(definition);
    }

    /**
     * Alias for {@link #registerValue(ValueDefinition)}.
     *
     * @param definition the value definition to register
     */
    @ApiStatus.Stable
    public static void addValue(ValueDefinition definition) {
        registerValue(definition);
    }

    /**
     * Registers a source item's value classification, mapping a source to a
     * specific value with a given contribution amount.
     *
     * @param sourceId      the registry identifier of the source item
     * @param valueKey the value key this source contributes to
     * @param amount      the value contribution amount per consumption
     * @throws IllegalArgumentException if the value key is not registered
     */
    public static void registerSourceClassification(ResourceLocation sourceId, String valueKey, float amount) {
        MarieAPIState.assertRegistrationAllowed("registerSourceClassification");
        dev.marie.MariesLib.util.MarieValidation.requireNonNullId(sourceId, "MarieAPI.registerSourceClassification");
        if (!Float.isFinite(amount)) {
            throw new IllegalArgumentException("MarieAPI.registerSourceClassification.amount: value must be finite, got " + amount);
        }
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(sourceId)) {
            org.slf4j.LoggerFactory.getLogger(MarieAPI.class).warn("[MarieAPI] registerSourceClassification: item '{}' not found in BuiltInRegistries.ITEM", sourceId);
        }
        MarieRegistryUtils.requireValueKey(valueKey, "MarieAPI.registerSourceClassification");
        MarieLibRegistrationDelegate delegate = MarieLibContext.get().registrationDelegate();
        if (delegate == null) {
            throw new IllegalStateException("MarieLib registration delegate not configured");
        }
        delegate.registerSourceClassification(sourceId, valueKey, amount);
    }

    /**
     * Alias for {@link #registerSourceClassification(ResourceLocation, String, float)}.
     *
     * @param sourceId      the registry identifier of the source item
     * @param valueKey the value key this source contributes to
     * @param amount      the value contribution amount per consumption
     */
    @ApiStatus.Stable
    public static void registerSource(ResourceLocation sourceId, String valueKey, float amount) {
        registerSourceClassification(sourceId, valueKey, amount);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Effects & Thresholds
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a custom effect triggered by value threshold crossings.
     *
     * @param definition the effect definition describing the trigger and effect
     * @throws IllegalArgumentException if the referenced value or effect doesn't exist
     */
    public static void registerCustomEffect(ThresholdEffect definition) {
        MarieAPIState.assertRegistrationAllowed("registerCustomEffect");
        MarieLibRegistrationDelegate delegate = MarieLibContext.get().registrationDelegate();
        if (delegate == null) {
            throw new IllegalStateException("MarieLib registration delegate not configured");
        }
        delegate.registerEffect(definition);
    }

    /**
     * Alias for {@link #registerCustomEffect(ThresholdEffect)}.
     *
     * @param definition the effect definition describing the trigger and effect
     */
    @ApiStatus.Stable
    public static void addEffect(ThresholdEffect definition) {
        registerCustomEffect(definition);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Compatibility
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a compatibility entry that maps source items from another mod
     * to MarieLib value keys.
     *
     * @param definition the compat definition with source-to-value mappings
     */
    public static void registerCompatEntry(CompatDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerCompatEntry");
        dev.marie.MariesLib.compat.ModCompat.registerExternal(definition);
    }

    /**
     * Alias for {@link #registerCompatEntry(CompatDefinition)}.
     *
     * @param definition the compat definition with source-to-value mappings
     */
    @ApiStatus.Stable
    public static void addCompat(CompatDefinition definition) {
        registerCompatEntry(definition);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Synergies & Combos
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a value synergy interaction between two values.
     * When both values meet their conditions simultaneously, the synergy
     * effect is applied.
     *
     * @param definition the value synergy definition
     * @throws IllegalArgumentException if referenced values don't exist
     */
    public static void registerValueSynergy(SynergyDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerValueSynergy");
        SynergyRegistry.registerValueSynergy(definition);
    }

    /**
     * Alias for {@link #registerValueSynergy(SynergyDefinition)}.
     *
     * @param definition the value synergy definition
     */
    @ApiStatus.Stable
    public static void addValueSynergy(SynergyDefinition definition) {
        registerValueSynergy(definition);
    }

    /**
     * Registers a source synergy (source pair) that grants a bonus value burst
     * when two sources are consumed within a time window.
     *
     * @param definition the source synergy definition
     */
    public static void registerSourcePairSynergy(SourcePairSynergy definition) {
        MarieAPIState.assertRegistrationAllowed("registerSourcePairSynergy");
        SynergyRegistry.registerSourcePairSynergy(definition);
    }

    /**
     * Alias for {@link #registerSourcePairSynergy(SourcePairSynergy)}.
     *
     * @param definition the source synergy definition
     */
    @ApiStatus.Stable
    public static void addSourceSynergy(SourcePairSynergy definition) {
        registerSourcePairSynergy(definition);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Profiles & Milestones
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a named tracking profile archetype that players can switch between.
     *
     * @param definition the tracking profile definition with custom thresholds and bonuses
     * @throws IllegalArgumentException if a profile with the same id already exists
     */
    public static void registerTrackingProfile(ProfileDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerTrackingProfile");
        ProfileRegistry.register(definition);
    }

    /**
     * Alias for {@link #registerTrackingProfile(ProfileDefinition)}.
     *
     * @param definition the tracking profile definition with custom thresholds and bonuses
     */
    @ApiStatus.Stable
    public static void addProfile(ProfileDefinition definition) {
        registerTrackingProfile(definition);
    }

    /**
     * Registers a value milestone that fires once when a player reaches
     * a cumulative value goal.
     *
     * @param definition the milestone definition
     * @throws IllegalArgumentException if a milestone with the same id already exists
     */
    public static void registerMilestone(MilestoneDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerMilestone");
        MilestoneRegistry.register(definition);
    }

    /**
     * Alias for {@link #registerMilestone(MilestoneDefinition)}.
     *
     * @param definition the milestone definition
     */
    @ApiStatus.Stable
    public static void addMilestone(MilestoneDefinition definition) {
        registerMilestone(definition);
    }

    // ───────────────────────────────────────────────────────────────
    // Registration — Hooks & Modifiers
    // ───────────────────────────────────────────────────────────────

    /**
     * Registers a season hook for integrating with Serene Seasons or similar mods.
     * Seasonal modifiers will be applied to value decay and absorption rates.
     *
     * @param hook the season hook implementation
     */
    public static void registerSeasonHook(MarieSeasonHook hook) {
        MarieAPIState.assertRegistrationAllowed("registerSeasonHook");
        SeasonHookRegistry.register(hook);
    }

    /**
     * Alias for {@link #registerSeasonHook(MarieSeasonHook)}.
     *
     * @param hook the season hook implementation
     */
    @ApiStatus.Stable
    public static void addSeasonHook(MarieSeasonHook hook) {
        registerSeasonHook(hook);
    }

    /**
     * Registers a value absorption modifier that dynamically adjusts how much
     * of a value a player absorbs based on their current state.
     *
     * @param modifier the absorption modifier implementation
     */
    public static void registerAbsorptionModifier(AbsorptionModifier modifier) {
        MarieAPIState.assertRegistrationAllowed("registerAbsorptionModifier");
        AbsorptionModifierRegistry.register(modifier);
    }

    /**
     * Alias for {@link #registerAbsorptionModifier(AbsorptionModifier)}.
     *
     * @param modifier the absorption modifier implementation
     */
    @ApiStatus.Stable
    public static void addAbsorptionModifier(AbsorptionModifier modifier) {
        registerAbsorptionModifier(modifier);
    }

    /**
     * Registers a tracking report provider that injects custom sections into the
     * {@code /marie} command report output.
     *
     * @param provider the report provider implementation
     */
    public static void registerReportProvider(ReportProvider provider) {
        MarieAPIState.assertRegistrationAllowed("registerReportProvider");
        ReportProviderRegistry.register(provider);
    }

    /**
     * Alias for {@link #registerReportProvider(ReportProvider)}.
     *
     * @param provider the report provider implementation
     */
    @ApiStatus.Stable
    public static void addReportSection(ReportProvider provider) {
        registerReportProvider(provider);
    }

    /**
     * Registers a source property signal used by the scanner to classify
     * items. The consuming mod provides all signal logic — the lib has
     * no opinion on what item properties mean.
     */
    @ApiStatus.Stable
    public static void registerSourcePropertySignal(SourcePropertySignal signal) {
        MarieAPIState.assertRegistrationAllowed("registerSourcePropertySignal");
        if (signal == null) {
            throw new IllegalArgumentException("signal cannot be null");
        }
        SourcePropertySignalRegistry.register(signal);
    }

    /**
     * Registers a sleep bonus evaluator. When a player wakes up,
     * MarieLib calls the evaluator and applies any returned effect.
     * Only one evaluator should be registered per mod; registering
     * multiple evaluators is supported and all are called in order.
     */
    @ApiStatus.Stable
    public static void registerSleepBonusEvaluator(SleepBonusEvaluator evaluator) {
        MarieAPIState.assertRegistrationAllowed("registerSleepBonusEvaluator");
        if (evaluator == null) {
            throw new IllegalArgumentException("evaluator cannot be null");
        }
        SleepBonusEvaluatorRegistry.register(evaluator);
    }

    /**
     * Registers a consuming-mod trigger listener. MarieLib will call
     * {@link SourceTriggerListener#register(IEventBus)} during
     * server event bus setup so the listener can subscribe its own
     * NeoForge event listeners.
     *
     * <p>This is optional — consuming mods may instead call
     * {@link #fireSourceTrigger} directly from their own event handlers
     * without using this registration.</p>
     */
    @ApiStatus.Stable
    public static void registerTriggerHandler(SourceTriggerListener handler) {
        MarieAPIState.assertRegistrationAllowed("registerTriggerHandler");
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }
        TriggerHandlerRegistry.register(handler);
    }

    /**
     * Registers a non-item source contribution for a specific trigger type.
     * When {@link #fireSourceTrigger} is called with a matching trigger,
     * the registered amounts are added to the pipeline.
     *
     * <p>Use this for EMC transactions, crafting, block breaking, etc.</p>
     */
    @ApiStatus.Stable
    public static void registerTriggerSource(SourceTriggerDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerTriggerSource");
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        MarieRegistryUtils.requireValueKey(definition.getValueKey(), "registerTriggerSource");
        SourceTriggerRegistry.register(definition);
    }

    /** Alias for registerTriggerSource. */
    @ApiStatus.Stable
    public static void addTriggerSource(SourceTriggerDefinition definition) {
        registerTriggerSource(definition);
    }

    /**
     * Fires the value pipeline for a custom trigger on a player.
     * Use this to contribute values from non-item actions (crafting,
     * EMC transactions, block breaking, etc.).
     *
     * <p>Must be called server-side. The trigger will be processed through
     * the full pipeline including absorption modifiers, synergies, and
     * threshold checks.</p>
     *
     * @param player  the server-side player receiving the value
     * @param trigger the trigger describing the source action
     */
    @ApiStatus.Stable
    public static void fireSourceTrigger(ServerPlayer player, ValueSourceTrigger trigger) {
        fireSourceTrigger(player, trigger, null);
    }

    /**
     * Fires the value pipeline for a custom trigger on a player.
     * For {@link ValueSourceTrigger.TriggerType#ITEM_CONSUMED}, pass the consumed stack so
     * classification lookup can run.
     *
     * @param player  the server-side player receiving the value
     * @param trigger the trigger describing the source action
     * @param stack   the item stack for item-based triggers, or {@code null} otherwise
     */
    @ApiStatus.Stable
    public static void fireSourceTrigger(ServerPlayer player, ValueSourceTrigger trigger, @Nullable ItemStack stack) {
        if (player == null || trigger == null) return;
        if (!TrackingAttachment.isRegistered()) return;
        TrackingData tracking = TrackingAttachment.getData(player);
        long gameTimeMs = player.level().getGameTime() * 50L;
        tracking.tickTime(gameTimeMs);
        tracking.tick();
        SourceApplicationPipeline.process(player, trigger, stack, tracking, gameTimeMs);
    }
}
