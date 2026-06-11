package dev.marie.MariesLib.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.ValueSourceTrigger;
import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.classification.ClassificationTrace;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.data.SchemaDefinition;
import dev.marie.MariesLib.runtime.SourceOverrideRegistry;
import dev.marie.MariesLib.scan.ResolutionStageHandler;
import dev.marie.MariesLib.scanner.ClassificationResult;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.tracking.TrackingMemoryConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

/**
 * Runtime context injected by the consuming mod at bootstrap.
 * Holds the mod ID and config-derived values that MarieLib cannot own.
 */
@ApiStatus.Stable
public final class MarieLibContext {

    public record SourceDelta(float total, Map<String, Float> values) {}

    @FunctionalInterface
    public interface SourceDeltaResolver {
        /**
         * Resolves value deltas for a source item.
         *
         * @param stack       the item stack being applied (may be null for
         *                    non-item triggers)
         * @param level       the current level
         * @param payload     the numeric payload from the trigger (consuming
         *                    mod defines what this means — could be nutrition,
         *                    EMC value, damage dealt, anything)
         * @param matchedBars the pre-resolved value bar weights for this item
         * @return a SourceDelta with total and per-value amounts
         */
        SourceDelta resolve(ItemStack stack, Level level, double payload,
                Map<String, Float> matchedBars);
    }

    private static volatile MarieLibContext instance;

    private final String modId;
    private final Supplier<Float> scannerConfidenceSpreadThreshold;
    private final Supplier<Float> compositeRatioThreshold;
    private final Supplier<Boolean> scannerEnableRecipeInheritance;
    private final Supplier<Boolean> enableDebugLogging;
    private final Supplier<List<String>> valueKeys;
    private final Consumer<Map<ResourceLocation, Map<String, Float>>> scannerApplyCallback;
    private final Supplier<Predicate<ItemStack>> valueTagChecker;
    private final Supplier<Predicate<ItemStack>> sourceItemFilter;
    private final Supplier<Long> memoryWindowMinutes;
    private final Supplier<Integer> memoryWindowCount;
    private final Supplier<Long> streakWindowMs;
    private final Supplier<Float> streakWeight;
    private final Supplier<Float> debtThreshold;
    private final Supplier<Float> debtDecayRate;
    private final Supplier<Float> diminishingSteepness;
    private final Supplier<Float> diminishingMidpoint;
    private final Supplier<Boolean> debugMemoryLogging;
    private final Supplier<Predicate<String>> isValueBeneficial;
    private final Supplier<Float> excessThreshold;
    private final Supplier<Float> lowThreshold;
    private final Supplier<Float> criticalThreshold;
    private final Runnable onFullTrackingDataSynced;
    private final Supplier<Screen> configScreenFactory;
    private final Function<Screen, Screen> exportScreenFactory;
    private final Function<Screen, Screen> importScreenFactory;
    private final Consumer<Map<String, Float>> onValuesDeltaReceived;
    private final Supplier<TrackingMemoryConfig> clientMemoryConfigProvider;
    private final Supplier<JsonObject> configExporter;
    private final Consumer<JsonObject> configImporter;
    private final Supplier<PresetRegistry.PresetValues> currentConfigPresetValues;
    private final Runnable ensureBuiltInPresetsOnDisk;
    private final Consumer<PresetRegistry.PresetValues> applyPresetValues;
    private final Runnable enableAllEffectsForPresets;
    private final Function<String, String> valueIconProvider;
    private final BiFunction<ItemStack, Player, Map<String, Float>> tooltipValueResolver;
    private final Supplier<TrackingData> clientTrackingDataProvider;
    private final Function<String, Integer> valueColorProvider;
    private final Function<ResourceLocation, String> sourceFamilyResolver;
    private final Function<ItemStack, Boolean> isSourceResolvable;
    private final Function<Item, Map<String, Float>> valueTagScoresProvider;
    private final Function<String, String> tagRoleResolver;
    private final BiPredicate<ServerPlayer, ValueSourceTrigger> heavySourceBlocker;
    private final BiPredicate<ServerPlayer, ValueSourceTrigger> lightSourceBlocker;
    private final DoubleSupplier multiValueInheritanceThreshold;
    private final ResolutionStageHandler[] runtimeResolverStages;
    private final String[] stemmerDictionary;
    private final Map<String, String[]> stemmerCompoundSplits;
    private final Map<String, String> stemmerIrregularForms;
    private final Set<String> stemmerStopWords;
    private final Supplier<Set<String>> stemmerNoiseSuffixes;
    private final Runnable onServerStarting;
    private final Consumer<MinecraftServer> onReloadBroadcast;
    private final Consumer<RecipeManager> onRecipeManagerBound;
    private final Runnable onRecipeManagerCleared;
    private final Runnable onCacheInvalidated;
    private final Supplier<TrackingMemoryConfig> trackingMemoryConfigProvider;
    private final BiFunction<ItemStack, Level, Map<String, Float>> sourceValueResolver;
    private final SourceDeltaResolver sourceDeltaResolver;
    private final Function<String, SourceOverrideRegistry.SourceOverride> sourceOverrideLookup;
    private final Function<ResourceLocation, Map<String, Float>> externalClassificationProvider;
    private final BiConsumer<ServerPlayer, TrackingData> effectApplier;
    private final Consumer<ServerPlayer> effectClearer;
    private final Function<String, Float> valueDecayRateProvider;
    private final Supplier<Integer> decayIntervalTicks;
    private final Function<String, Float> criticalThresholdProvider;
    private final Supplier<Boolean> showJoinMessage;
    private final Supplier<Component> joinMessageLine1;
    private final Supplier<Component> joinMessageLine2;
    private final Supplier<Set<String>> previousEffectIds;
    private final Predicate<String> effectDefinitionRegistered;
    private final BiConsumer<ServerPlayer, TrackingData> trackingDeltaSyncer;
    private final Consumer<ServerPlayer> syncOnJoin;
    private final BiFunction<ItemStack, RecipeManager, String> heldItemTraceProvider;
    private final BiFunction<ItemStack, RecipeManager, ClassificationTrace> heldItemClassificationTraceProvider;
    private final Supplier<List<ClassificationResult>> classifiedSourceProvider;
    private final Supplier<List<SchemaDefinition>> schemaProviders;
    @Nullable
    private final MarieLibPlayerDataProvider playerDataProvider;
    @Nullable
    private final MarieLibRegistrationDelegate registrationDelegate;

    private MarieLibContext(Builder builder) {
        this.modId = builder.modId;
        this.scannerConfidenceSpreadThreshold = builder.scannerConfidenceSpreadThreshold;
        this.compositeRatioThreshold = builder.compositeRatioThreshold;
        this.scannerEnableRecipeInheritance = builder.scannerEnableRecipeInheritance;
        this.enableDebugLogging = builder.enableDebugLogging;
        this.valueKeys = builder.valueKeys;
        this.scannerApplyCallback = builder.scannerApplyCallback;
        this.valueTagChecker = builder.valueTagChecker;
        this.sourceItemFilter = builder.sourceItemFilter;
        this.memoryWindowMinutes = builder.memoryWindowMinutes;
        this.memoryWindowCount = builder.memoryWindowCount;
        this.streakWindowMs = builder.streakWindowMs;
        this.streakWeight = builder.streakWeight;
        this.debtThreshold = builder.debtThreshold;
        this.debtDecayRate = builder.debtDecayRate;
        this.diminishingSteepness = builder.diminishingSteepness;
        this.diminishingMidpoint = builder.diminishingMidpoint;
        this.debugMemoryLogging = builder.debugMemoryLogging;
        this.isValueBeneficial = builder.isValueBeneficial;
        this.excessThreshold = builder.excessThreshold;
        this.lowThreshold = builder.lowThreshold;
        this.criticalThreshold = builder.criticalThreshold;
        this.onFullTrackingDataSynced = builder.onFullTrackingDataSynced;
        this.configScreenFactory = builder.configScreenFactory;
        this.exportScreenFactory = builder.exportScreenFactory;
        this.importScreenFactory = builder.importScreenFactory;
        this.onValuesDeltaReceived = builder.onValuesDeltaReceived;
        this.clientMemoryConfigProvider = builder.clientMemoryConfigProvider;
        this.configExporter = builder.configExporter;
        this.configImporter = builder.configImporter;
        this.currentConfigPresetValues = builder.currentConfigPresetValues;
        this.ensureBuiltInPresetsOnDisk = builder.ensureBuiltInPresetsOnDisk;
        this.applyPresetValues = builder.applyPresetValues;
        this.enableAllEffectsForPresets = builder.enableAllEffectsForPresets;
        this.valueIconProvider = builder.valueIconProvider;
        this.tooltipValueResolver = builder.tooltipValueResolver;
        this.clientTrackingDataProvider = builder.clientTrackingDataProvider;
        this.valueColorProvider = builder.valueColorProvider;
        this.sourceFamilyResolver = builder.sourceFamilyResolver;
        this.isSourceResolvable = builder.isSourceResolvable;
        this.valueTagScoresProvider = builder.valueTagScoresProvider;
        this.tagRoleResolver = builder.tagRoleResolver;
        this.heavySourceBlocker = builder.heavySourceBlocker;
        this.lightSourceBlocker = builder.lightSourceBlocker;
        this.multiValueInheritanceThreshold = builder.multiValueInheritanceThreshold;
        this.runtimeResolverStages = builder.runtimeResolverStages;
        this.stemmerDictionary = builder.stemmerDictionary;
        this.stemmerCompoundSplits = builder.stemmerCompoundSplits;
        this.stemmerIrregularForms = builder.stemmerIrregularForms;
        this.stemmerStopWords = builder.stemmerStopWords;
        this.stemmerNoiseSuffixes = builder.stemmerNoiseSuffixes;
        this.onServerStarting = builder.onServerStarting;
        this.onReloadBroadcast = builder.onReloadBroadcast;
        this.onRecipeManagerBound = builder.onRecipeManagerBound;
        this.onRecipeManagerCleared = builder.onRecipeManagerCleared;
        this.onCacheInvalidated = builder.onCacheInvalidated;
        this.trackingMemoryConfigProvider = builder.trackingMemoryConfigProvider;
        this.sourceValueResolver = builder.sourceValueResolver;
        this.sourceDeltaResolver = builder.sourceDeltaResolver;
        this.sourceOverrideLookup = builder.sourceOverrideLookup;
        this.externalClassificationProvider = builder.externalClassificationProvider;
        this.effectApplier = builder.effectApplier;
        this.effectClearer = builder.effectClearer;
        this.valueDecayRateProvider = builder.valueDecayRateProvider;
        this.decayIntervalTicks = builder.decayIntervalTicks;
        this.criticalThresholdProvider = builder.criticalThresholdProvider;
        this.showJoinMessage = builder.showJoinMessage;
        this.joinMessageLine1 = builder.joinMessageLine1;
        this.joinMessageLine2 = builder.joinMessageLine2;
        this.previousEffectIds = builder.previousEffectIds;
        this.effectDefinitionRegistered = builder.effectDefinitionRegistered;
        this.trackingDeltaSyncer = builder.trackingDeltaSyncer;
        this.syncOnJoin = builder.syncOnJoin;
        this.heldItemTraceProvider = builder.heldItemTraceProvider;
        this.heldItemClassificationTraceProvider = builder.heldItemClassificationTraceProvider;
        this.classifiedSourceProvider = builder.classifiedSourceProvider;
        this.schemaProviders = builder.schemaProviders;
        this.playerDataProvider = builder.playerDataProvider;
        this.registrationDelegate = builder.registrationDelegate;
    }

    public static void register(MarieLibContext context) {
        instance = context;
    }

    public static MarieLibContext get() {
        MarieLibContext ctx = instance;
        if (ctx == null) throw new IllegalStateException("MarieLibContext not registered");
        return ctx;
    }

    public static boolean isRegistered() {
        return instance != null;
    }

    public String modId() { return modId; }
    public float scannerConfidenceSpreadThreshold() { return scannerConfidenceSpreadThreshold.get(); }
    public float compositeRatioThreshold() { return compositeRatioThreshold.get(); }
    public boolean scannerEnableRecipeInheritance() { return scannerEnableRecipeInheritance.get(); }
    public boolean enableDebugLogging() { return enableDebugLogging.get(); }
    public List<String> valueKeys() { return valueKeys.get(); }
    public void applyScannerResults(Map<ResourceLocation, Map<String, Float>> results) {
        scannerApplyCallback.accept(results);
    }
    public Predicate<ItemStack> valueTagChecker() { return valueTagChecker.get(); }
    public Predicate<ItemStack> sourceItemFilter() { return sourceItemFilter.get(); }
    public long memoryWindowMinutes() { return memoryWindowMinutes.get(); }
    public int memoryWindowCount() { return memoryWindowCount.get(); }
    public long streakWindowMs() { return streakWindowMs.get(); }
    public float streakWeight() { return streakWeight.get(); }
    public float debtThreshold() { return debtThreshold.get(); }
    public float debtDecayRate() { return debtDecayRate.get(); }
    public float diminishingSteepness() { return diminishingSteepness.get(); }
    public float diminishingMidpoint() { return diminishingMidpoint.get(); }
    public boolean debugMemoryLogging() { return debugMemoryLogging.get(); }
    public Predicate<String> isValueBeneficial() { return isValueBeneficial.get(); }
    public float excessThreshold() { return excessThreshold.get(); }
    public float lowThreshold() { return lowThreshold.get(); }
    public float criticalThreshold() { return criticalThreshold.get(); }
    public void onFullTrackingDataSynced() { onFullTrackingDataSynced.run(); }
    public Screen configScreenFactory() { return configScreenFactory.get(); }
    public Screen exportScreenFactory(Screen parent) { return exportScreenFactory.apply(parent); }
    public Screen importScreenFactory(Screen parent) { return importScreenFactory.apply(parent); }
    public void onValuesDeltaReceived(Map<String, Float> delta) { onValuesDeltaReceived.accept(delta); }
    public TrackingMemoryConfig clientMemoryConfigProvider() { return clientMemoryConfigProvider.get(); }
    public JsonObject configExporter() { return configExporter.get(); }
    public void configImporter(JsonObject json) { configImporter.accept(json); }
    public PresetRegistry.PresetValues currentConfigPresetValues() { return currentConfigPresetValues.get(); }
    public void ensureBuiltInPresetsOnDisk() { ensureBuiltInPresetsOnDisk.run(); }
    public void applyPresetValues(PresetRegistry.PresetValues values) { applyPresetValues.accept(values); }
    public void enableAllEffectsForPresets() { enableAllEffectsForPresets.run(); }
    public String valueIcon(String key) { return valueIconProvider.apply(key); }
    public BiFunction<ItemStack, Player, Map<String, Float>> tooltipValueResolver() { return tooltipValueResolver; }
    public Supplier<TrackingData> clientTrackingDataProvider() { return clientTrackingDataProvider; }
    public Function<String, Integer> valueColorProvider() { return valueColorProvider; }
    public Function<ResourceLocation, String> sourceFamilyResolver() { return sourceFamilyResolver; }
    public boolean isSourceResolvable(ItemStack stack) { return isSourceResolvable.apply(stack); }
    public Function<Item, Map<String, Float>> valueTagScoresProvider() { return valueTagScoresProvider; }

    /**
     * Resolves a named tag role to a full tag path for this mod's domain.
     * The consuming mod maps well-known role keys to their actual tag paths.
     *
     * Built-in role keys used by MarieLib internally:
     *   "source_override"  — items that bypass scanner classification
     *   "heavy_source"     — items the pipeline treats as heavy
     *   "light_source"     — items the pipeline treats as light
     *
     * The consuming mod may register any additional roles it needs.
     * Returns null if the role is not mapped, which means no tag filtering
     * is applied for that role.
     */
    @Nullable
    public String resolveTagRole(String role) {
        return tagRoleResolver.apply(role);
    }

    /**
     * Predicate that decides whether a source trigger should be blocked
     * because it is considered "heavy" for the current player state.
     * The consuming mod defines what "heavy" means — the lib just asks.
     *
     * Return true to block the trigger, false to allow it.
     * Default: never block (always returns false).
     */
    public boolean isHeavySourceBlocked(ServerPlayer player, ValueSourceTrigger trigger) {
        return heavySourceBlocker.test(player, trigger);
    }

    /**
     * Predicate that decides whether a source trigger should be blocked
     * because it is considered "light" for the current player state.
     * Default: never block (always returns false).
     */
    public boolean isLightSourceBlocked(ServerPlayer player, ValueSourceTrigger trigger) {
        return lightSourceBlocker.test(player, trigger);
    }

    public double multiValueInheritanceThreshold() { return multiValueInheritanceThreshold.getAsDouble(); }
    public ResolutionStageHandler[] runtimeResolverStages() { return runtimeResolverStages; }
    public String[] stemmerDictionary() { return stemmerDictionary; }
    public Map<String, String[]> stemmerCompoundSplits() { return stemmerCompoundSplits; }
    public Map<String, String> stemmerIrregularForms() { return stemmerIrregularForms; }
    public Set<String> stemmerStopWords() { return stemmerStopWords; }
    public Set<String> stemmerNoiseSuffixes() { return stemmerNoiseSuffixes.get(); }
    public void onServerStarting() { onServerStarting.run(); }
    public void onReloadBroadcast(MinecraftServer server) { onReloadBroadcast.accept(server); }
    public void onRecipeManagerBound(RecipeManager recipeManager) { onRecipeManagerBound.accept(recipeManager); }
    public void onRecipeManagerCleared() { onRecipeManagerCleared.run(); }
    public void onCacheInvalidated() { onCacheInvalidated.run(); }
    public Supplier<TrackingMemoryConfig> trackingMemoryConfigProvider() { return trackingMemoryConfigProvider; }
    public BiFunction<ItemStack, Level, Map<String, Float>> sourceValueResolver() { return sourceValueResolver; }
    public SourceDeltaResolver sourceDeltaResolver() { return sourceDeltaResolver; }
    public Function<String, SourceOverrideRegistry.SourceOverride> sourceOverrideLookup() { return sourceOverrideLookup; }
    public Function<ResourceLocation, Map<String, Float>> externalClassificationProvider() { return externalClassificationProvider; }
    public BiConsumer<ServerPlayer, TrackingData> effectApplier() { return effectApplier; }
    public Consumer<ServerPlayer> effectClearer() { return effectClearer; }
    public Function<String, Float> valueDecayRateProvider() { return valueDecayRateProvider; }
    public int decayIntervalTicks() { return decayIntervalTicks.get(); }
    public float criticalThresholdFor(String valueKey) { return criticalThresholdProvider.apply(valueKey); }
    public boolean showJoinMessage() { return showJoinMessage.get(); }
    public Component joinMessageLine1() { return joinMessageLine1.get(); }
    public Component joinMessageLine2() { return joinMessageLine2.get(); }
    public Set<String> previousEffectIds() { return previousEffectIds.get(); }
    public boolean isEffectDefinitionRegistered(String effectId) { return effectDefinitionRegistered.test(effectId); }
    public BiConsumer<ServerPlayer, TrackingData> trackingDeltaSyncer() { return trackingDeltaSyncer; }
    public Consumer<ServerPlayer> syncOnJoin() { return syncOnJoin; }
    public BiFunction<ItemStack, RecipeManager, String> heldItemTraceProvider() { return heldItemTraceProvider; }
    public BiFunction<ItemStack, RecipeManager, ClassificationTrace> heldItemClassificationTraceProvider() {
        return heldItemClassificationTraceProvider;
    }
    public Supplier<List<ClassificationResult>> classifiedSourceProvider() { return classifiedSourceProvider; }
    public Supplier<List<SchemaDefinition>> schemaProviders() { return schemaProviders; }
    @Nullable
    public MarieLibPlayerDataProvider playerDataProvider() { return playerDataProvider; }
    @Nullable
    public MarieLibRegistrationDelegate registrationDelegate() { return registrationDelegate; }

    /**
     * Returns the ValueDefinition for the given key, or null if not registered.
     */
    @Nullable
    public ValueDefinition valueDefinitionFor(String key) {
        ValueDefinition registered = ValueRegistry.get(key);
        if (registered != null) {
            return registered;
        }
        if (registrationDelegate != null) {
            return registrationDelegate.valueDefinitionFor(key);
        }
        return null;
    }

    public static Builder builder(String modId) { return new Builder(modId); }

    public static final class Builder {
        private final String modId;
        private Supplier<Float> scannerConfidenceSpreadThreshold = () -> 0f;
        private Supplier<Float> compositeRatioThreshold = () -> 0f;
        private Supplier<Boolean> scannerEnableRecipeInheritance = () -> false;
        private Supplier<Boolean> enableDebugLogging = () -> false;
        private Supplier<List<String>> valueKeys = List::of;
        private Consumer<Map<ResourceLocation, Map<String, Float>>> scannerApplyCallback = ignored -> {};
        private Supplier<Predicate<ItemStack>> valueTagChecker = () -> stack -> false;
        private Supplier<Predicate<ItemStack>> sourceItemFilter = () -> stack -> true;
        private Supplier<Long> memoryWindowMinutes = () -> 60L;
        private Supplier<Integer> memoryWindowCount = () -> 20;
        private Supplier<Long> streakWindowMs = () -> 300_000L;
        private Supplier<Float> streakWeight = () -> 1.5f;
        private Supplier<Float> debtThreshold = () -> 5f;
        private Supplier<Float> debtDecayRate = () -> 0.01f;
        private Supplier<Float> diminishingSteepness = () -> 1.0f;
        private Supplier<Float> diminishingMidpoint = () -> 3.0f;
        private Supplier<Boolean> debugMemoryLogging = () -> false;
        private Supplier<Predicate<String>> isValueBeneficial = () -> key -> true;
        private Supplier<Float> excessThreshold = () -> 0.9f;
        private Supplier<Float> lowThreshold = () -> 0.3f;
        private Supplier<Float> criticalThreshold = () -> 0.25f;
        private Runnable onFullTrackingDataSynced = () -> {};
        private Supplier<Screen> configScreenFactory = () -> null;
        private Function<Screen, Screen> exportScreenFactory = parent -> null;
        private Function<Screen, Screen> importScreenFactory = parent -> null;
        private Consumer<Map<String, Float>> onValuesDeltaReceived = delta -> {};
        private Supplier<TrackingMemoryConfig> clientMemoryConfigProvider =
                () -> new TrackingMemoryConfig(60L, 1.2, 3.0, 0.2, 0.5);
        private Supplier<JsonObject> configExporter = JsonObject::new;
        private Consumer<JsonObject> configImporter = json -> {};
        private Supplier<PresetRegistry.PresetValues> currentConfigPresetValues = PresetRegistry.PresetValues::empty;
        private Runnable ensureBuiltInPresetsOnDisk = () -> {};
        private Consumer<PresetRegistry.PresetValues> applyPresetValues = values -> {};
        private Runnable enableAllEffectsForPresets = () -> {};
        private Function<String, String> valueIconProvider = key -> "minecraft:apple";
        private BiFunction<ItemStack, Player, Map<String, Float>> tooltipValueResolver = (stack, player) -> Map.of();
        private Supplier<TrackingData> clientTrackingDataProvider = () -> new TrackingData();
        private Function<String, Integer> valueColorProvider = key -> 0xFFFFFFFF;
        private Function<ResourceLocation, String> sourceFamilyResolver = id -> null;
        private Function<ItemStack, Boolean> isSourceResolvable = stack -> true;
        private Function<Item, Map<String, Float>> valueTagScoresProvider = item -> Map.of();
        private Function<String, String> tagRoleResolver = role -> null;
        private BiPredicate<ServerPlayer, ValueSourceTrigger> heavySourceBlocker =
                (player, trigger) -> false;
        private BiPredicate<ServerPlayer, ValueSourceTrigger> lightSourceBlocker =
                (player, trigger) -> false;
        private DoubleSupplier multiValueInheritanceThreshold = () -> 0.20;
        private ResolutionStageHandler[] runtimeResolverStages = new ResolutionStageHandler[0];
        private String[] stemmerDictionary = new String[0];
        private Map<String, String[]> stemmerCompoundSplits = Map.of();
        private Map<String, String> stemmerIrregularForms = Map.of();
        private Set<String> stemmerStopWords = Set.of();
        private Supplier<Set<String>> stemmerNoiseSuffixes = Set::of;
        private Runnable onServerStarting = () -> {};
        private Consumer<MinecraftServer> onReloadBroadcast = server -> {};
        private Consumer<RecipeManager> onRecipeManagerBound = rm -> {};
        private Runnable onRecipeManagerCleared = () -> {};
        private Runnable onCacheInvalidated = () -> {};
        private Supplier<TrackingMemoryConfig> trackingMemoryConfigProvider = () -> null;
        private BiFunction<ItemStack, Level, Map<String, Float>> sourceValueResolver = (stack, level) -> Map.of();
        private SourceDeltaResolver sourceDeltaResolver =
                (stack, level, payload, bars) -> new SourceDelta(0f, Map.of());
        private Function<String, SourceOverrideRegistry.SourceOverride> sourceOverrideLookup = id -> null;
        private Function<ResourceLocation, Map<String, Float>> externalClassificationProvider = id -> null;
        private BiConsumer<ServerPlayer, TrackingData> effectApplier = (p, d) -> {};
        private Consumer<ServerPlayer> effectClearer = p -> {};
        private Function<String, Float> valueDecayRateProvider = key -> 0f;
        private Supplier<Integer> decayIntervalTicks = () -> 20;
        private Function<String, Float> criticalThresholdProvider = key -> 0.25f;
        private Supplier<Boolean> showJoinMessage = () -> false;
        private Supplier<Component> joinMessageLine1 = () -> Component.empty();
        private Supplier<Component> joinMessageLine2 = () -> Component.empty();
        private Supplier<Set<String>> previousEffectIds = Set::of;
        private Predicate<String> effectDefinitionRegistered = id -> false;
        private BiConsumer<ServerPlayer, TrackingData> trackingDeltaSyncer = (p, d) -> {};
        private Consumer<ServerPlayer> syncOnJoin = p -> {};
        private BiFunction<ItemStack, RecipeManager, String> heldItemTraceProvider = (stack, rm) -> "";
        private BiFunction<ItemStack, RecipeManager, ClassificationTrace> heldItemClassificationTraceProvider =
                (stack, rm) -> null;
        private Supplier<List<ClassificationResult>> classifiedSourceProvider = List::of;
        private Supplier<List<SchemaDefinition>> schemaProviders = () -> List.of(
                SchemaDefinition.forValue(),
                SchemaDefinition.forSourceClassification(),
                SchemaDefinition.forEffect(),
                SchemaDefinition.forSynergy(),
                SchemaDefinition.forSourcePairSynergy(),
                SchemaDefinition.forMilestone(),
                SchemaDefinition.forTrackingProfile(),
                SchemaDefinition.forCompat()
        );
        @Nullable
        private MarieLibPlayerDataProvider playerDataProvider;
        @Nullable
        private MarieLibRegistrationDelegate registrationDelegate;

        private Builder(String modId) { this.modId = modId; }

        public Builder scannerConfidenceSpreadThreshold(Supplier<Float> s) { this.scannerConfidenceSpreadThreshold = s; return this; }
        public Builder compositeRatioThreshold(Supplier<Float> s) { this.compositeRatioThreshold = s; return this; }
        public Builder scannerEnableRecipeInheritance(Supplier<Boolean> s) { this.scannerEnableRecipeInheritance = s; return this; }
        public Builder enableDebugLogging(Supplier<Boolean> s) { this.enableDebugLogging = s; return this; }
        public Builder valueKeys(Supplier<List<String>> s) { this.valueKeys = s; return this; }
        public Builder scannerApplyCallback(Consumer<Map<ResourceLocation, Map<String, Float>>> c) { this.scannerApplyCallback = c; return this; }
        public Builder valueTagChecker(Supplier<Predicate<ItemStack>> s) { this.valueTagChecker = s; return this; }
        public Builder sourceItemFilter(Supplier<Predicate<ItemStack>> s) { this.sourceItemFilter = s; return this; }
        public Builder memoryWindowMinutes(Supplier<Long> s) { this.memoryWindowMinutes = s; return this; }
        public Builder memoryWindowCount(Supplier<Integer> s) { this.memoryWindowCount = s; return this; }
        public Builder streakWindowMs(Supplier<Long> s) { this.streakWindowMs = s; return this; }
        public Builder streakWeight(Supplier<Float> s) { this.streakWeight = s; return this; }
        public Builder debtThreshold(Supplier<Float> s) { this.debtThreshold = s; return this; }
        public Builder debtDecayRate(Supplier<Float> s) { this.debtDecayRate = s; return this; }
        public Builder diminishingSteepness(Supplier<Float> s) { this.diminishingSteepness = s; return this; }
        public Builder diminishingMidpoint(Supplier<Float> s) { this.diminishingMidpoint = s; return this; }
        public Builder debugMemoryLogging(Supplier<Boolean> s) { this.debugMemoryLogging = s; return this; }
        public Builder isValueBeneficial(Supplier<Predicate<String>> s) { this.isValueBeneficial = s; return this; }
        public Builder excessThreshold(Supplier<Float> s) { this.excessThreshold = s; return this; }
        public Builder lowThreshold(Supplier<Float> s) { this.lowThreshold = s; return this; }
        public Builder criticalThreshold(Supplier<Float> s) { this.criticalThreshold = s; return this; }
        public Builder onFullTrackingDataSynced(Runnable r) { this.onFullTrackingDataSynced = r; return this; }
        public Builder configScreenFactory(Supplier<Screen> s) { this.configScreenFactory = s; return this; }
        public Builder exportScreenFactory(Function<Screen, Screen> f) { this.exportScreenFactory = f; return this; }
        public Builder importScreenFactory(Function<Screen, Screen> f) { this.importScreenFactory = f; return this; }
        public Builder onValuesDeltaReceived(Consumer<Map<String, Float>> c) { this.onValuesDeltaReceived = c; return this; }
        public Builder clientMemoryConfigProvider(Supplier<TrackingMemoryConfig> s) { this.clientMemoryConfigProvider = s; return this; }
        public Builder configExporter(Supplier<JsonObject> s) { this.configExporter = s; return this; }
        public Builder configImporter(Consumer<JsonObject> c) { this.configImporter = c; return this; }
        public Builder currentConfigPresetValues(Supplier<PresetRegistry.PresetValues> s) { this.currentConfigPresetValues = s; return this; }
        public Builder ensureBuiltInPresetsOnDisk(Runnable r) { this.ensureBuiltInPresetsOnDisk = r; return this; }
        public Builder applyPresetValues(Consumer<PresetRegistry.PresetValues> c) { this.applyPresetValues = c; return this; }
        public Builder enableAllEffectsForPresets(Runnable r) { this.enableAllEffectsForPresets = r; return this; }
        public Builder valueIconProvider(Function<String, String> f) { this.valueIconProvider = f; return this; }
        public Builder tooltipValueResolver(BiFunction<ItemStack, Player, Map<String, Float>> f) { this.tooltipValueResolver = f; return this; }
        public Builder clientTrackingDataProvider(Supplier<TrackingData> s) { this.clientTrackingDataProvider = s; return this; }
        public Builder valueColorProvider(Function<String, Integer> f) { this.valueColorProvider = f; return this; }
        public Builder sourceFamilyResolver(Function<ResourceLocation, String> f) { this.sourceFamilyResolver = f; return this; }
        public Builder isSourceResolvable(Function<ItemStack, Boolean> f) { this.isSourceResolvable = f; return this; }
        public Builder valueTagScoresProvider(Function<Item, Map<String, Float>> f) { this.valueTagScoresProvider = f; return this; }
        public Builder tagRoleResolver(Function<String, String> f) { this.tagRoleResolver = f; return this; }
        public Builder heavySourceBlocker(BiPredicate<ServerPlayer, ValueSourceTrigger> p) {
            this.heavySourceBlocker = p;
            return this;
        }
        public Builder lightSourceBlocker(BiPredicate<ServerPlayer, ValueSourceTrigger> p) {
            this.lightSourceBlocker = p;
            return this;
        }
        public Builder multiValueInheritanceThreshold(DoubleSupplier s) { this.multiValueInheritanceThreshold = s; return this; }
        public Builder runtimeResolverStages(ResolutionStageHandler[] stages) { this.runtimeResolverStages = stages; return this; }
        public Builder stemmerDictionary(String[] dictionary) { this.stemmerDictionary = dictionary; return this; }
        public Builder stemmerCompoundSplits(Map<String, String[]> splits) { this.stemmerCompoundSplits = splits; return this; }
        public Builder stemmerIrregularForms(Map<String, String> forms) { this.stemmerIrregularForms = forms; return this; }
        public Builder stemmerStopWords(Set<String> stopWords) { this.stemmerStopWords = stopWords; return this; }
        public Builder stemmerNoiseSuffixes(Supplier<Set<String>> s) { this.stemmerNoiseSuffixes = s; return this; }
        public Builder onServerStarting(Runnable r) { this.onServerStarting = r; return this; }
        public Builder onReloadBroadcast(Consumer<MinecraftServer> c) { this.onReloadBroadcast = c; return this; }
        public Builder onRecipeManagerBound(Consumer<RecipeManager> c) { this.onRecipeManagerBound = c; return this; }
        public Builder onRecipeManagerCleared(Runnable r) { this.onRecipeManagerCleared = r; return this; }
        public Builder onCacheInvalidated(Runnable r) { this.onCacheInvalidated = r; return this; }
        public Builder trackingMemoryConfigProvider(Supplier<TrackingMemoryConfig> s) { this.trackingMemoryConfigProvider = s; return this; }
        public Builder sourceValueResolver(BiFunction<ItemStack, Level, Map<String, Float>> f) { this.sourceValueResolver = f; return this; }
        public Builder sourceDeltaResolver(SourceDeltaResolver r) { this.sourceDeltaResolver = r; return this; }
        public Builder sourceOverrideLookup(Function<String, SourceOverrideRegistry.SourceOverride> f) { this.sourceOverrideLookup = f; return this; }
        public Builder externalClassificationProvider(Function<ResourceLocation, Map<String, Float>> f) { this.externalClassificationProvider = f; return this; }
        public Builder effectApplier(BiConsumer<ServerPlayer, TrackingData> c) { this.effectApplier = c; return this; }
        public Builder effectClearer(Consumer<ServerPlayer> c) { this.effectClearer = c; return this; }
        public Builder valueDecayRateProvider(Function<String, Float> f) { this.valueDecayRateProvider = f; return this; }
        public Builder decayIntervalTicks(Supplier<Integer> s) { this.decayIntervalTicks = s; return this; }
        public Builder criticalThresholdProvider(Function<String, Float> f) { this.criticalThresholdProvider = f; return this; }
        public Builder showJoinMessage(Supplier<Boolean> s) { this.showJoinMessage = s; return this; }
        public Builder joinMessageLine1(Supplier<Component> s) { this.joinMessageLine1 = s; return this; }
        public Builder joinMessageLine2(Supplier<Component> s) { this.joinMessageLine2 = s; return this; }
        public Builder previousEffectIds(Supplier<Set<String>> s) { this.previousEffectIds = s; return this; }
        public Builder effectDefinitionRegistered(Predicate<String> p) { this.effectDefinitionRegistered = p; return this; }
        public Builder trackingDeltaSyncer(BiConsumer<ServerPlayer, TrackingData> c) { this.trackingDeltaSyncer = c; return this; }
        public Builder syncOnJoin(Consumer<ServerPlayer> c) { this.syncOnJoin = c; return this; }
        public Builder heldItemTraceProvider(BiFunction<ItemStack, RecipeManager, String> f) {
            this.heldItemTraceProvider = f;
            return this;
        }
        public Builder heldItemClassificationTraceProvider(
                BiFunction<ItemStack, RecipeManager, ClassificationTrace> f) {
            this.heldItemClassificationTraceProvider = f;
            return this;
        }
        public Builder classifiedSourceProvider(Supplier<List<ClassificationResult>> s) {
            this.classifiedSourceProvider = s;
            return this;
        }
        public Builder schemaProviders(Supplier<List<SchemaDefinition>> s) {
            this.schemaProviders = s;
            return this;
        }
        public Builder playerDataProvider(MarieLibPlayerDataProvider p) { this.playerDataProvider = p; return this; }
        public Builder registrationDelegate(MarieLibRegistrationDelegate d) { this.registrationDelegate = d; return this; }

        public MarieLibContext build() { return new MarieLibContext(this); }
    }
}
