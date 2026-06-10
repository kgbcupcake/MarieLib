package dev.marie.MariesLib.api;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPIState;
import dev.marie.MariesLib.api.MarieAPIVersion;
import dev.marie.MariesLib.api.impl.EmptyMemoryView;
import dev.marie.MariesLib.compat.CompatDefinition;
import dev.marie.MariesLib.api.registry.AbsorptionModifierRegistry;
import dev.marie.MariesLib.api.registry.ProfileRegistry;
import dev.marie.MariesLib.api.registry.MilestoneRegistry;
import dev.marie.MariesLib.api.registry.ReportProviderRegistry;
import dev.marie.MariesLib.api.registry.SeasonHookRegistry;
import dev.marie.MariesLib.api.registry.SynergyRegistry;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MarieLibPlayerDataProvider;
import dev.marie.MariesLib.core.MarieLibRegistrationDelegate;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
        return ResourceLocation.fromNamespaceAndPath(MarieLibContext.get().modId(), "api");
    }

    private MarieAPI() {}

    // ───────────────────────────────────────────────────────────────
    // Player State Queries
    // ───────────────────────────────────────────────────────────────

    /**
     * Returns the total total count for the given player based on their
     * current value levels and recent source consumption.
     *
     * @param player the player to query
     * @return the player's current total value
     * @throws IllegalStateException if the value system is not initialized
     */
    public static float getTotal(Player player) {
        if (player == null) {
            return 0f;
        }
        MarieLibPlayerDataProvider provider = MarieLibContext.get().playerDataProvider();
        if (provider == null) {
            return 0f;
        }
        return provider.getTotal(player);
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
        MarieLibPlayerDataProvider provider = MarieLibContext.get().playerDataProvider();
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
     * @return a {@link MemoryView} for the given player
     * @throws IllegalStateException if the value system is not initialized
     */
    public static MemoryView getSourceMemory(Player player) {
        if (player == null) {
            return EmptyMemoryView.INSTANCE;
        }
        MarieLibPlayerDataProvider provider = MarieLibContext.get().playerDataProvider();
        if (provider == null) {
            return EmptyMemoryView.INSTANCE;
        }
        return provider.getSourceMemoryView(player);
    }
    /**
     * Alias for {@link #getTotal(Player)}.
     *
     * @param player the player to query
     * @return the player's current total value
     */
    @ApiStatus.Stable
    public static float getTotalCount(Player player) {
        return getTotal(player);
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
                getTotal(player),
                Collections.unmodifiableMap(values),
                getSourceMemory(player)
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
        MarieLibPlayerDataProvider provider = MarieLibContext.get().playerDataProvider();
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
        MarieLibRegistrationDelegate delegate = MarieLibContext.get().registrationDelegate();
        if (delegate == null) {
            throw new IllegalStateException("MarieLib registration delegate not configured");
        }
        if (delegate.getValueKeys().contains(definition.getId())) {
            throw new IllegalArgumentException("Value already registered: " + definition.getId());
        }
        delegate.registerValue(definition);
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
        dev.marie.MariesLib.util.MarieValidation.requireNonNullId(sourceId, "MarieAPI.registerSourceClassification");
        dev.marie.MariesLib.util.MarieValidation.requireFinite(amount, -10f, 10f, "MarieAPI.registerSourceClassification.amount");
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
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
        if (!MarieAPIState.isRegistrationAllowed()) throw new IllegalStateException("MarieAPI registration is closed — register during mod initialization only.");
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
}
