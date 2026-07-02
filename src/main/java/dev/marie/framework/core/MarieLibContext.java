package dev.marie.MariesLib.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import dev.marie.MariesLib.api.ValueModifierContext;
import dev.marie.MariesLib.api.ValueSourceTrigger;
import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.config.MariesLibConfigBridge;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.tracking.AttachmentTrackingDataProvider;
import dev.marie.MariesLib.runtime.SourceClassificationRegistry;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import dev.marie.MariesLib.util.MarieValidation;
import dev.marie.MariesLib.scan.ResolutionStageHandler;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.tracking.DiminishingReturnsConfig;
import dev.marie.MariesLib.tracking.DeathNutritionBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Runtime context injected by the consuming mod at bootstrap.
 * Holds the mod ID and optional overrides that MarieLib cannot own.
 *
 * <p>Only {@link Builder#build()} requires {@code modId}; every other builder field has a
 * safe lib-owned default. Use {@link MariesLibBootstrap#attach} for zero-config wiring.</p>
 */
@ApiStatus.Internal
public final class MarieLibContext implements MarieLibSettings, IMarieLibConfig {

    @ApiStatus.Internal
    public record SourceDelta(float total, Map<String, Float> values) {}

    @ApiStatus.Experimental
    @FunctionalInterface
    public interface SourceDeltaResolver {
        /**
         * Resolves value deltas for a source item.
         *
         * @param stack       the item stack being applied (may be null for
         *                    non-item triggers)
         * @param level       the current level
         * @param payload     the numeric payload from the trigger (consuming
         *                    mod defines what this means — could be energy,
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
    private final Supplier<Float> excessThreshold;
    private final Supplier<Float> lowThreshold;
    private final Supplier<Float> criticalThreshold;
    private final Runnable onFullTrackingDataSynced;
    private final Supplier<Object> configScreenFactory;
    private final Function<Object, Object> exportScreenFactory;
    private final Function<Object, Object> importScreenFactory;
    private final Consumer<Map<String, Float>> onValuesDeltaReceived;
    private final Supplier<DiminishingReturnsConfig> clientMemoryConfigProvider;
    private final Supplier<JsonObject> configExporter;
    private final Consumer<JsonObject> configImporter;
    private final Supplier<PresetRegistry.PresetValues> currentConfigPresetValues;
    private final Runnable ensureBuiltInPresetsOnDisk;
    private final Consumer<PresetRegistry.PresetValues> applyPresetValues;
    private final Runnable enableAllEffectsForPresets;
    private final Function<String, String> valueIconProvider;
    private final BiFunction<ItemStack, Player, Map<String, Float>> tooltipValueResolver;
    private final Supplier<TrackingData> clientTrackingDataProvider;
    private final Function<ResourceLocation, String> sourceFamilyResolver;
    private final Function<Item, Map<String, Float>> valueTagScoresProvider;
    private final Function<String, String> tagRoleResolver;
    private final BiPredicate<ServerPlayer, ValueSourceTrigger> heavySourceBlocker;
    private final BiPredicate<ServerPlayer, ValueSourceTrigger> lightSourceBlocker;
    private final DoubleSupplier multiValueInheritanceThreshold;
    private final ResolutionStageHandler[] runtimeResolverStages;
    private final Supplier<DiminishingReturnsConfig> trackingMemoryConfigProvider;
    private final BiFunction<ItemStack, Level, Map<String, Float>> sourceValueResolver;
    private final SourceDeltaResolver sourceDeltaResolver;
    private final BiConsumer<ServerPlayer, TrackingData> effectApplier;
    private final Consumer<ServerPlayer> effectClearer;
    private final Supplier<Integer> decayIntervalTicks;
    private final Function<String, Float> decayRateResolver;
    private final Supplier<Boolean> showJoinMessage;
    private final Supplier<Component> joinMessageLine1;
    private final Supplier<Component> joinMessageLine2;
    private final BiConsumer<ServerPlayer, TrackingData> trackingDeltaSyncer;
    private final Consumer<ServerPlayer> syncOnJoin;
    private final Supplier<DeathNutritionBehavior> deathNutritionBehavior;
    @Nullable
    private final BiConsumer<ServerPlayer, TrackingData> deathNutritionHandler;
    @Nullable
    private final MarieLibDataProvider dataProvider;
    @Nullable
    private final MarieLibRegistrationDelegate registrationDelegate;
    private final Runnable cacheInvalidatedHook;
    private final Consumer<MinecraftServer> reloadBroadcastHook;
    private final BiFunction<ValueModifierContext, Float, Float> postValueModifierHook;

    private MarieLibContext(Builder builder) {
        this.modId = builder.modId;
        this.scannerConfidenceSpreadThreshold = builder.scannerConfidenceSpreadThreshold;
        this.compositeRatioThreshold = builder.compositeRatioThreshold;
        this.scannerEnableRecipeInheritance = builder.scannerEnableRecipeInheritance;
        this.enableDebugLogging = builder.enableDebugLogging;
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
        this.sourceFamilyResolver = builder.sourceFamilyResolver;
        this.valueTagScoresProvider = builder.valueTagScoresProvider;
        this.tagRoleResolver = builder.tagRoleResolver;
        this.heavySourceBlocker = builder.heavySourceBlocker;
        this.lightSourceBlocker = builder.lightSourceBlocker;
        this.multiValueInheritanceThreshold = builder.multiValueInheritanceThreshold;
        this.runtimeResolverStages = builder.runtimeResolverStages;
        this.trackingMemoryConfigProvider = builder.trackingMemoryConfigProvider;
        this.sourceValueResolver = builder.sourceValueResolver;
        this.sourceDeltaResolver = builder.sourceDeltaResolver;
        this.effectApplier = builder.effectApplier;
        this.effectClearer = builder.effectClearer;
        this.decayIntervalTicks = builder.decayIntervalTicks;
        this.decayRateResolver = builder.decayRateResolver;
        this.showJoinMessage = builder.showJoinMessage;
        this.joinMessageLine1 = builder.joinMessageLine1;
        this.joinMessageLine2 = builder.joinMessageLine2;
        this.trackingDeltaSyncer = builder.trackingDeltaSyncer;
        this.syncOnJoin = builder.syncOnJoin;
        this.deathNutritionBehavior = builder.deathNutritionBehavior;
        this.deathNutritionHandler = builder.deathNutritionHandler;
        this.dataProvider = builder.dataProvider != null
                ? builder.dataProvider
                : new AttachmentTrackingDataProvider();
        this.registrationDelegate = builder.registrationDelegate;
        this.cacheInvalidatedHook = builder.cacheInvalidatedHook;
        this.reloadBroadcastHook = builder.reloadBroadcastHook;
        this.postValueModifierHook = builder.postValueModifierHook;
    }

    @ApiStatus.Stable
    public static void register(MarieLibContext context) {
        instance = context;
        MarieModRegistry.register(context);
    }

    @ApiStatus.Stable
    public static MarieLibContext get() {
        MarieLibContext ctx = instance;
        if (ctx == null) {
            throw new IllegalStateException("MarieLibContext not registered");
        }
        return ctx;
    }

    @ApiStatus.Stable
    public static boolean isRegistered() {
        return instance != null;
    }

    @Override
    @ApiStatus.Stable
    public String modId() {
        return modId;
    }

    @Override
    @ApiStatus.Internal
    public float scannerConfidenceSpreadThreshold() {
        return scannerConfidenceSpreadThreshold.get();
    }

    @Override
    @ApiStatus.Internal
    public float compositeRatioThreshold() {
        return compositeRatioThreshold.get();
    }

    @Override
    @ApiStatus.Internal
    public boolean scannerEnableRecipeInheritance() {
        return scannerEnableRecipeInheritance.get();
    }

    @Override
    @ApiStatus.Internal
    public boolean enableDebugLogging() {
        return enableDebugLogging.get();
    }

    @ApiStatus.Stable
    public List<String> valueKeys() {
        return ValueRegistry.getAll()
                .stream()
                .map(ValueDefinition::getId)
                .toList();
    }

    @ApiStatus.Internal
    public Predicate<ItemStack> sourceItemFilter() {
        return sourceItemFilter.get();
    }

    @ApiStatus.Internal
    public long memoryWindowMinutes() {
        return memoryWindowMinutes.get();
    }

    @ApiStatus.Internal
    public int memoryWindowCount() {
        return memoryWindowCount.get();
    }

    @ApiStatus.Internal
    public long streakWindowMs() {
        return streakWindowMs.get();
    }

    @ApiStatus.Internal
    public float streakWeight() {
        return streakWeight.get();
    }

    @ApiStatus.Internal
    public float debtThreshold() {
        return debtThreshold.get();
    }

    @ApiStatus.Internal
    public float debtDecayRate() {
        return debtDecayRate.get();
    }

    @ApiStatus.Internal
    public float diminishingSteepness() {
        return diminishingSteepness.get();
    }

    @ApiStatus.Internal
    public float diminishingMidpoint() {
        return diminishingMidpoint.get();
    }

    @ApiStatus.Internal
    public boolean debugMemoryLogging() {
        return debugMemoryLogging.get();
    }

    @ApiStatus.Internal
    public float excessThreshold() {
        return excessThreshold.get();
    }

    @ApiStatus.Internal
    public float lowThreshold() {
        return lowThreshold.get();
    }

    @ApiStatus.Internal
    public float criticalThreshold() {
        return criticalThreshold.get();
    }

    @ApiStatus.Internal
    public void onFullTrackingDataSynced() {
        onFullTrackingDataSynced.run();
    }

    @ApiStatus.Internal
    public Object configScreenFactory() {
        return configScreenFactory.get();
    }

    @ApiStatus.Internal
    public Object exportScreenFactory(Object parent) {
        return exportScreenFactory.apply(parent);
    }

    @ApiStatus.Internal
    public Object importScreenFactory(Object parent) {
        return importScreenFactory.apply(parent);
    }

    @ApiStatus.Internal
    public void onValuesDeltaReceived(Map<String, Float> delta) {
        onValuesDeltaReceived.accept(delta);
    }

    @ApiStatus.Internal
    public DiminishingReturnsConfig clientMemoryConfigProvider() {
        return clientMemoryConfigProvider.get();
    }

    @ApiStatus.Internal
    public JsonObject configExporter() {
        return configExporter.get();
    }

    @ApiStatus.Internal
    public void configImporter(JsonObject json) {
        configImporter.accept(json);
    }

    @ApiStatus.Internal
    public PresetRegistry.PresetValues currentConfigPresetValues() {
        return currentConfigPresetValues.get();
    }

    @ApiStatus.Internal
    public void ensureBuiltInPresetsOnDisk() {
        ensureBuiltInPresetsOnDisk.run();
    }

    @ApiStatus.Internal
    public void applyPresetValues(PresetRegistry.PresetValues values) {
        applyPresetValues.accept(values);
    }

    @ApiStatus.Internal
    public void enableAllEffectsForPresets() {
        enableAllEffectsForPresets.run();
    }

    @ApiStatus.Internal
    public String valueIcon(String key) {
        return valueIconProvider.apply(key);
    }

    @ApiStatus.Internal
    public BiFunction<ItemStack, Player, Map<String, Float>> tooltipValueResolver() {
        return tooltipValueResolver;
    }

    @ApiStatus.Internal
    public Supplier<TrackingData> clientTrackingDataProvider() {
        return clientTrackingDataProvider;
    }

    @ApiStatus.Internal
    public Function<ResourceLocation, String> sourceFamilyResolver() {
        return sourceFamilyResolver;
    }

    @ApiStatus.Internal
    public Function<Item, Map<String, Float>> valueTagScoresProvider() {
        return valueTagScoresProvider;
    }

    /**
     * Resolves a named tag role to a full tag path for this mod's domain.
     * The consuming mod maps well-known role keys to their actual tag paths.
     *
     * The consuming mod may register any role keys it needs (e.g. "source_override").
     * Returns null if the role is not mapped, which means no tag filtering
     * is applied for that role.
     */
    @Nullable
    @ApiStatus.Internal
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
    @ApiStatus.Internal
    public boolean isHeavySourceBlocked(ServerPlayer player, ValueSourceTrigger trigger) {
        return heavySourceBlocker.test(player, trigger);
    }

    /**
     * Predicate that decides whether a source trigger should be blocked
     * because it is considered "light" for the current player state.
     * Default: never block (always returns false).
     */
    @ApiStatus.Internal
    public boolean isLightSourceBlocked(ServerPlayer player, ValueSourceTrigger trigger) {
        return lightSourceBlocker.test(player, trigger);
    }

    @Override
    @ApiStatus.Internal
    public double multiValueInheritanceThreshold() {
        return multiValueInheritanceThreshold.getAsDouble();
    }

    @ApiStatus.Internal
    public ResolutionStageHandler[] runtimeResolverStages() {
        return runtimeResolverStages;
    }

    @ApiStatus.Internal
    public DiminishingReturnsConfig trackingMemoryConfig() {
        DiminishingReturnsConfig cfg = trackingMemoryConfigProvider.get();
        return cfg != null ? cfg : defaultDiminishingReturnsConfig();
    }

    @ApiStatus.Internal
    public BiFunction<ItemStack, Level, Map<String, Float>> sourceValueResolver() {
        return sourceValueResolver;
    }

    @ApiStatus.Internal
    public SourceDeltaResolver sourceDeltaResolver() {
        return sourceDeltaResolver;
    }

    @ApiStatus.Internal
    public BiConsumer<ServerPlayer, TrackingData> effectApplier() {
        return effectApplier;
    }

    @ApiStatus.Internal
    public Consumer<ServerPlayer> effectClearer() {
        return effectClearer;
    }

    @ApiStatus.Internal
    public int decayIntervalTicks() {
        return decayIntervalTicks.get();
    }

    @ApiStatus.Internal
    public float decayRateFor(String valueKey) {
        return decayRateResolver.apply(valueKey);
    }

    @ApiStatus.Internal
    public float criticalThresholdFor(String valueKey) {
        ValueDefinition def = ValueRegistry.get(valueKey);
        return def != null ? def.getCriticalThreshold() : criticalThreshold();
    }

    @ApiStatus.Internal
    public boolean showJoinMessage() {
        return showJoinMessage.get();
    }

    @ApiStatus.Internal
    public Component joinMessageLine1() {
        return joinMessageLine1.get();
    }

    @ApiStatus.Internal
    public Component joinMessageLine2() {
        return joinMessageLine2.get();
    }

    @ApiStatus.Internal
    public BiConsumer<ServerPlayer, TrackingData> trackingDeltaSyncer() {
        return trackingDeltaSyncer;
    }

    @ApiStatus.Internal
    public Consumer<ServerPlayer> syncOnJoin() {
        return syncOnJoin;
    }

    /**
     * Default death respawn policy when {@link #deathNutritionHandler()} is not set.
     */
    @ApiStatus.Stable
    public Supplier<DeathNutritionBehavior> deathNutritionBehavior() {
        return deathNutritionBehavior;
    }

    /**
     * Optional override for death respawn tracking adjustments. When non-null, replaces
     * {@link #deathNutritionBehavior()} entirely. The consumer should mutate {@code tracking}
     * in place; sync runs afterward via {@link #syncOnJoin()}.
     */
    @Nullable
    @ApiStatus.Stable
    public BiConsumer<ServerPlayer, TrackingData> deathNutritionHandler() {
        return deathNutritionHandler;
    }

    @ApiStatus.Experimental
    public Runnable cacheInvalidatedHook() {
        return cacheInvalidatedHook;
    }

    @ApiStatus.Experimental
    public Consumer<MinecraftServer> reloadBroadcastHook() {
        return reloadBroadcastHook;
    }

    @ApiStatus.Internal
    public float applyPostValueModifier(ValueModifierContext ctx, float amount) {
        return postValueModifierHook.apply(ctx, amount);
    }

    @Nullable
    @ApiStatus.Internal
    public MarieLibDataProvider dataProvider() {
        return dataProvider;
    }

    /**
     * @deprecated Use {@link dev.marie.MariesLib.api.MarieAPI#registerValue} and related
     *             {@code MarieAPI.register*} methods directly instead of supplying a delegate.
     */
    @Nullable
    @Deprecated
    @ApiStatus.Internal
    public MarieLibRegistrationDelegate registrationDelegate() {
        return registrationDelegate;
    }

    /**
     * Returns the ValueDefinition for the given key, or null if not registered.
     */
    @Nullable
    @ApiStatus.Stable
    public ValueDefinition valueDefinitionFor(String key) {
        return ValueRegistry.get(key);
    }

    @ApiStatus.Internal
    public static boolean isValueBeneficial(String valueKey) {
        ValueDefinition def = ValueRegistry.get(valueKey);
        return def == null || def.isBeneficial();
    }

    @ApiStatus.Stable
    public static Builder builder(String modId) {
        return new Builder(modId);
    }

    private static DiminishingReturnsConfig defaultDiminishingReturnsConfig() {
        return new DiminishingReturnsConfig(60L, 1.2, 3.0, 0.2, 0.5);
    }

    private static Map<String, Float> defaultSourceValueResolver(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) {
            return Map.of();
        }
        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack);
        if (itemId == null) {
            return Map.of();
        }
        Map<String, Float> result = new HashMap<>();
        for (ValueDefinition def : ValueRegistry.getAll()) {
            float score = SourceClassificationRegistry.getScore(itemId.toString(), def.getId());
            if (score != 0f) {
                result.put(def.getId(), score);
            }
        }
        return result.isEmpty() ? Map.of() : result;
    }

    private static Map<String, Float> defaultTooltipValueResolver(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty()) {
            return Map.of();
        }
        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack);
        if (itemId == null) {
            return Map.of();
        }
        Map<String, Float> result = new HashMap<>();
        for (ValueDefinition def : ValueRegistry.getAll()) {
            float score = SourceClassificationRegistry.getScore(itemId.toString(), def.getId());
            if (score != 0f) {
                result.put(def.getId(), score);
            }
        }
        return result.isEmpty() ? Map.of() : result;
    }

    private static SourceDelta defaultSourceDeltaResolver(
            ItemStack stack,
            Level level,
            double payload,
            Map<String, Float> bars
    ) {
        if (bars == null || bars.isEmpty()) {
            return new SourceDelta(0f, Map.of());
        }
        Map<String, Float> deltas = new HashMap<>();
        float total = 0f;
        for (Map.Entry<String, Float> entry : bars.entrySet()) {
            String key = entry.getKey();
            float barWeight = entry.getValue();
            ValueDefinition def = ValueRegistry.get(key);
            double scale = def != null ? def.getAmountScale() : 1.0;
            float delta = (float) (payload * barWeight / scale);
            deltas.put(key, delta);
            total += delta;
        }
        return new SourceDelta(total, deltas);
    }

    public static final class Builder {
        private final String modId;
        private Supplier<Float> scannerConfidenceSpreadThreshold = () -> 0f;
        private Supplier<Float> compositeRatioThreshold = () -> 0f;
        private Supplier<Boolean> scannerEnableRecipeInheritance = () -> false;
        private Supplier<Boolean> enableDebugLogging = () -> false;
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
        private Supplier<Float> excessThreshold = () -> 0.9f;
        private Supplier<Float> lowThreshold = () -> 0.3f;
        private Supplier<Float> criticalThreshold = () -> 0.25f;
        private Runnable onFullTrackingDataSynced = () -> {};
        private Supplier<Object> configScreenFactory = () -> null;
        private Function<Object, Object> exportScreenFactory = parent -> null;
        private Function<Object, Object> importScreenFactory = parent -> null;
        private Consumer<Map<String, Float>> onValuesDeltaReceived = delta -> {};
        private Supplier<DiminishingReturnsConfig> clientMemoryConfigProvider = MarieLibContext::defaultDiminishingReturnsConfig;
        private Supplier<JsonObject> configExporter = MariesLibConfigBridge::buildExportRoot;
        private Consumer<JsonObject> configImporter = MariesLibConfigBridge::applyImport;
        private Supplier<PresetRegistry.PresetValues> currentConfigPresetValues = PresetRegistry.PresetValues::empty;
        private Runnable ensureBuiltInPresetsOnDisk = () -> {};
        private Consumer<PresetRegistry.PresetValues> applyPresetValues = values -> {};
        private Runnable enableAllEffectsForPresets = () -> {};
        private Function<String, String> valueIconProvider = key -> "minecraft:barrier";
        private BiFunction<ItemStack, Player, Map<String, Float>> tooltipValueResolver =
                MarieLibContext::defaultTooltipValueResolver;
        private Supplier<TrackingData> clientTrackingDataProvider = TrackingData::new;
        private Function<ResourceLocation, String> sourceFamilyResolver = id -> null;
        private Function<Item, Map<String, Float>> valueTagScoresProvider = item -> Map.of();
        private Function<String, String> tagRoleResolver = role -> null;
        private BiPredicate<ServerPlayer, ValueSourceTrigger> heavySourceBlocker =
                (player, trigger) -> false;
        private BiPredicate<ServerPlayer, ValueSourceTrigger> lightSourceBlocker =
                (player, trigger) -> false;
        private DoubleSupplier multiValueInheritanceThreshold = () -> 0.20;
        private ResolutionStageHandler[] runtimeResolverStages = new ResolutionStageHandler[0];
        private Supplier<DiminishingReturnsConfig> trackingMemoryConfigProvider = MarieLibContext::defaultDiminishingReturnsConfig;
        private BiFunction<ItemStack, Level, Map<String, Float>> sourceValueResolver =
                MarieLibContext::defaultSourceValueResolver;
        private SourceDeltaResolver sourceDeltaResolver = MarieLibContext::defaultSourceDeltaResolver;
        private BiConsumer<ServerPlayer, TrackingData> effectApplier = (p, d) -> {};
        private Consumer<ServerPlayer> effectClearer = p -> {};
        private Supplier<Integer> decayIntervalTicks = () -> 20;
        private Function<String, Float> decayRateResolver = valueKey -> {
            ValueDefinition def = ValueRegistry.get(valueKey);
            return def != null ? def.getDefaultDecayRate() : 0.001f;
        };
        private Supplier<Boolean> showJoinMessage = () -> false;
        private Supplier<Component> joinMessageLine1 = Component::empty;
        private Supplier<Component> joinMessageLine2 = Component::empty;
        private BiConsumer<ServerPlayer, TrackingData> trackingDeltaSyncer = (p, d) -> {};
        private Consumer<ServerPlayer> syncOnJoin = p -> {};
        private Supplier<DeathNutritionBehavior> deathNutritionBehavior = () -> DeathNutritionBehavior.PRESERVE;
        @Nullable
        private BiConsumer<ServerPlayer, TrackingData> deathNutritionHandler = null;
        @Nullable
        private MarieLibDataProvider dataProvider;
        @Nullable
        private MarieLibRegistrationDelegate registrationDelegate;
        private Runnable cacheInvalidatedHook = () -> {};
        private Consumer<MinecraftServer> reloadBroadcastHook = server -> {};
        private BiFunction<ValueModifierContext, Float, Float> postValueModifierHook = (ctx, amount) -> amount;

        private Builder(String modId) {
            this.modId = modId;
        }

        public Builder scannerConfidenceSpreadThreshold(Supplier<Float> s) { this.scannerConfidenceSpreadThreshold = s; return this; }
        public Builder compositeRatioThreshold(Supplier<Float> s) { this.compositeRatioThreshold = s; return this; }
        public Builder scannerEnableRecipeInheritance(Supplier<Boolean> s) { this.scannerEnableRecipeInheritance = s; return this; }
        public Builder enableDebugLogging(Supplier<Boolean> s) { this.enableDebugLogging = s; return this; }
        @ApiStatus.Experimental
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
        public Builder excessThreshold(Supplier<Float> s) { this.excessThreshold = s; return this; }
        public Builder lowThreshold(Supplier<Float> s) { this.lowThreshold = s; return this; }
        public Builder criticalThreshold(Supplier<Float> s) { this.criticalThreshold = s; return this; }
        @ApiStatus.Experimental
        public Builder onFullTrackingDataSynced(Runnable r) { this.onFullTrackingDataSynced = r; return this; }
        public Builder configScreenFactory(Supplier<Object> s) { this.configScreenFactory = s; return this; }
        public Builder exportScreenFactory(Function<Object, Object> f) { this.exportScreenFactory = f; return this; }
        public Builder importScreenFactory(Function<Object, Object> f) { this.importScreenFactory = f; return this; }
        @ApiStatus.Experimental
        public Builder onValuesDeltaReceived(Consumer<Map<String, Float>> c) { this.onValuesDeltaReceived = c; return this; }
        public Builder clientMemoryConfigProvider(Supplier<DiminishingReturnsConfig> s) { this.clientMemoryConfigProvider = s; return this; }
        public Builder configExporter(Supplier<JsonObject> s) { this.configExporter = s; return this; }
        public Builder configImporter(Consumer<JsonObject> c) { this.configImporter = c; return this; }
        public Builder currentConfigPresetValues(Supplier<PresetRegistry.PresetValues> s) { this.currentConfigPresetValues = s; return this; }
        public Builder ensureBuiltInPresetsOnDisk(Runnable r) { this.ensureBuiltInPresetsOnDisk = r; return this; }
        public Builder applyPresetValues(Consumer<PresetRegistry.PresetValues> c) { this.applyPresetValues = c; return this; }
        public Builder enableAllEffectsForPresets(Runnable r) { this.enableAllEffectsForPresets = r; return this; }
        public Builder valueIconProvider(Function<String, String> f) { this.valueIconProvider = f; return this; }
        @ApiStatus.Experimental
        public Builder tooltipValueResolver(BiFunction<ItemStack, Player, Map<String, Float>> f) { this.tooltipValueResolver = f; return this; }
        @ApiStatus.Experimental
        public Builder clientTrackingDataProvider(Supplier<TrackingData> s) { this.clientTrackingDataProvider = s; return this; }
        public Builder sourceFamilyResolver(Function<ResourceLocation, String> f) { this.sourceFamilyResolver = f; return this; }
        @ApiStatus.Experimental
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
        @ApiStatus.Experimental
        public Builder runtimeResolverStages(ResolutionStageHandler[] stages) { this.runtimeResolverStages = stages; return this; }
        public Builder trackingMemoryConfigProvider(Supplier<DiminishingReturnsConfig> s) { this.trackingMemoryConfigProvider = s; return this; }
        @ApiStatus.Experimental
        public Builder sourceValueResolver(BiFunction<ItemStack, Level, Map<String, Float>> f) { this.sourceValueResolver = f; return this; }
        @ApiStatus.Experimental
        public Builder sourceDeltaResolver(SourceDeltaResolver r) { this.sourceDeltaResolver = r; return this; }
        @ApiStatus.Experimental
        public Builder effectApplier(BiConsumer<ServerPlayer, TrackingData> c) { this.effectApplier = c; return this; }
        @ApiStatus.Experimental
        public Builder effectClearer(Consumer<ServerPlayer> c) { this.effectClearer = c; return this; }
        public Builder decayIntervalTicks(Supplier<Integer> s) { this.decayIntervalTicks = s; return this; }
        public Builder decayRateFor(Function<String, Float> resolver) { this.decayRateResolver = resolver; return this; }
        @ApiStatus.Experimental
        public Builder showJoinMessage(Supplier<Boolean> s) { this.showJoinMessage = s; return this; }
        @ApiStatus.Experimental
        public Builder joinMessageLine1(Supplier<Component> s) { this.joinMessageLine1 = s; return this; }
        @ApiStatus.Experimental
        public Builder joinMessageLine2(Supplier<Component> s) { this.joinMessageLine2 = s; return this; }
        @ApiStatus.Experimental
        public Builder trackingDeltaSyncer(BiConsumer<ServerPlayer, TrackingData> c) { this.trackingDeltaSyncer = c; return this; }
        @ApiStatus.Experimental
        public Builder syncOnJoin(Consumer<ServerPlayer> c) { this.syncOnJoin = c; return this; }
        /**
         * Death respawn policy when {@link #deathNutritionHandler(BiConsumer)} is not set.
         * Default: {@link DeathNutritionBehavior#PRESERVE}.
         */
        @ApiStatus.Stable
        public Builder deathNutritionBehavior(Supplier<DeathNutritionBehavior> s) {
            this.deathNutritionBehavior = s != null ? s : () -> DeathNutritionBehavior.PRESERVE;
            return this;
        }
        /**
         * Fully replaces {@link #deathNutritionBehavior(Supplier)} when set. Use for mod-specific
         * death handling (e.g. resetting auxiliary attachments) before diet sync on respawn.
         */
        @ApiStatus.Stable
        public Builder deathNutritionHandler(@Nullable BiConsumer<ServerPlayer, TrackingData> handler) {
            this.deathNutritionHandler = handler;
            return this;
        }
        @ApiStatus.Experimental
        public Builder onCacheInvalidated(Runnable hook) {
            this.cacheInvalidatedHook = hook != null ? hook : () -> {};
            return this;
        }
        @ApiStatus.Experimental
        public Builder onReloadBroadcast(Consumer<MinecraftServer> hook) {
            this.reloadBroadcastHook = hook != null ? hook : server -> {};
            return this;
        }
        @ApiStatus.Experimental
        public Builder postValueModifierHook(BiFunction<ValueModifierContext, Float, Float> hook) {
            this.postValueModifierHook = hook != null ? hook : (ctx, amount) -> amount;
            return this;
        }
        @ApiStatus.Stable
        public Builder dataProvider(MarieLibDataProvider p) { this.dataProvider = p; return this; }
        /**
         * @deprecated Use {@link dev.marie.MariesLib.api.MarieAPI#registerValue} and related
         *             {@code MarieAPI.register*} methods directly instead of supplying a delegate.
         */
        @Deprecated
        @ApiStatus.Internal
        public Builder registrationDelegate(MarieLibRegistrationDelegate d) { this.registrationDelegate = d; return this; }

        @ApiStatus.Stable
        public MarieLibContext build() {
            if (!MarieValidation.sanitizeModId(modId)) {
                throw new IllegalArgumentException(
                        "modId must match [a-z0-9_]{1,64}, got: '" + modId + "'");
            }
            return new MarieLibContext(this);
        }
    }
}
