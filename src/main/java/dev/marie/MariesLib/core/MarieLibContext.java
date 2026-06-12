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
import dev.marie.MariesLib.api.ValueSourceTrigger;
import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.config.MariesLibConfigBridge;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.runtime.SourceValueRegistry;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import dev.marie.MariesLib.scan.ResolutionStageHandler;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.tracking.DiminishingReturnsConfig;
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
@ApiStatus.Stable
public final class MarieLibContext implements MarieLibSettings, IMarieLibConfig {

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
    private final Supplier<Boolean> showJoinMessage;
    private final Supplier<Component> joinMessageLine1;
    private final Supplier<Component> joinMessageLine2;
    private final BiConsumer<ServerPlayer, TrackingData> trackingDeltaSyncer;
    private final Consumer<ServerPlayer> syncOnJoin;
    @Nullable
    private final MarieLibDataProvider dataProvider;
    @Nullable
    private final MarieLibRegistrationDelegate registrationDelegate;
    private final Runnable cacheInvalidatedHook;
    private final Consumer<MinecraftServer> reloadBroadcastHook;

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
        this.showJoinMessage = builder.showJoinMessage;
        this.joinMessageLine1 = builder.joinMessageLine1;
        this.joinMessageLine2 = builder.joinMessageLine2;
        this.trackingDeltaSyncer = builder.trackingDeltaSyncer;
        this.syncOnJoin = builder.syncOnJoin;
        this.dataProvider = builder.dataProvider;
        this.registrationDelegate = builder.registrationDelegate;
        this.cacheInvalidatedHook = builder.cacheInvalidatedHook;
        this.reloadBroadcastHook = builder.reloadBroadcastHook;
    }

    public static void register(MarieLibContext context) {
        instance = context;
        MarieModRegistry.register(context);
    }

    public static MarieLibContext get() {
        MarieLibContext ctx = instance;
        if (ctx == null) {
            throw new IllegalStateException("MarieLibContext not registered");
        }
        return ctx;
    }

    public static boolean isRegistered() {
        return instance != null;
    }

    @Override
    public String modId() {
        return modId;
    }

    @Override
    public float scannerConfidenceSpreadThreshold() {
        return scannerConfidenceSpreadThreshold.get();
    }

    @Override
    public float compositeRatioThreshold() {
        return compositeRatioThreshold.get();
    }

    @Override
    public boolean scannerEnableRecipeInheritance() {
        return scannerEnableRecipeInheritance.get();
    }

    @Override
    public boolean enableDebugLogging() {
        return enableDebugLogging.get();
    }

    public List<String> valueKeys() {
        return ValueRegistry.getAll()
                .stream()
                .map(ValueDefinition::getId)
                .toList();
    }

    public Predicate<ItemStack> sourceItemFilter() {
        return sourceItemFilter.get();
    }

    public long memoryWindowMinutes() {
        return memoryWindowMinutes.get();
    }

    public int memoryWindowCount() {
        return memoryWindowCount.get();
    }

    public long streakWindowMs() {
        return streakWindowMs.get();
    }

    public float streakWeight() {
        return streakWeight.get();
    }

    public float debtThreshold() {
        return debtThreshold.get();
    }

    public float debtDecayRate() {
        return debtDecayRate.get();
    }

    public float diminishingSteepness() {
        return diminishingSteepness.get();
    }

    public float diminishingMidpoint() {
        return diminishingMidpoint.get();
    }

    public boolean debugMemoryLogging() {
        return debugMemoryLogging.get();
    }

    public float excessThreshold() {
        return excessThreshold.get();
    }

    public float lowThreshold() {
        return lowThreshold.get();
    }

    public float criticalThreshold() {
        return criticalThreshold.get();
    }

    public void onFullTrackingDataSynced() {
        onFullTrackingDataSynced.run();
    }

    public Object configScreenFactory() {
        return configScreenFactory.get();
    }

    public Object exportScreenFactory(Object parent) {
        return exportScreenFactory.apply(parent);
    }

    public Object importScreenFactory(Object parent) {
        return importScreenFactory.apply(parent);
    }

    public void onValuesDeltaReceived(Map<String, Float> delta) {
        onValuesDeltaReceived.accept(delta);
    }

    public DiminishingReturnsConfig clientMemoryConfigProvider() {
        return clientMemoryConfigProvider.get();
    }

    public JsonObject configExporter() {
        return configExporter.get();
    }

    public void configImporter(JsonObject json) {
        configImporter.accept(json);
    }

    public PresetRegistry.PresetValues currentConfigPresetValues() {
        return currentConfigPresetValues.get();
    }

    public void ensureBuiltInPresetsOnDisk() {
        ensureBuiltInPresetsOnDisk.run();
    }

    public void applyPresetValues(PresetRegistry.PresetValues values) {
        applyPresetValues.accept(values);
    }

    public void enableAllEffectsForPresets() {
        enableAllEffectsForPresets.run();
    }

    public String valueIcon(String key) {
        return valueIconProvider.apply(key);
    }

    public BiFunction<ItemStack, Player, Map<String, Float>> tooltipValueResolver() {
        return tooltipValueResolver;
    }

    public Supplier<TrackingData> clientTrackingDataProvider() {
        return clientTrackingDataProvider;
    }

    public Function<ResourceLocation, String> sourceFamilyResolver() {
        return sourceFamilyResolver;
    }

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

    @Override
    public double multiValueInheritanceThreshold() {
        return multiValueInheritanceThreshold.getAsDouble();
    }

    public ResolutionStageHandler[] runtimeResolverStages() {
        return runtimeResolverStages;
    }

    public DiminishingReturnsConfig trackingMemoryConfig() {
        DiminishingReturnsConfig cfg = trackingMemoryConfigProvider.get();
        return cfg != null ? cfg : defaultDiminishingReturnsConfig();
    }

    public BiFunction<ItemStack, Level, Map<String, Float>> sourceValueResolver() {
        return sourceValueResolver;
    }

    public SourceDeltaResolver sourceDeltaResolver() {
        return sourceDeltaResolver;
    }

    public BiConsumer<ServerPlayer, TrackingData> effectApplier() {
        return effectApplier;
    }

    public Consumer<ServerPlayer> effectClearer() {
        return effectClearer;
    }

    public int decayIntervalTicks() {
        return decayIntervalTicks.get();
    }

    public float criticalThresholdFor(String valueKey) {
        ValueDefinition def = ValueRegistry.get(valueKey);
        return def != null ? def.getCriticalThreshold() : criticalThreshold();
    }

    public boolean showJoinMessage() {
        return showJoinMessage.get();
    }

    public Component joinMessageLine1() {
        return joinMessageLine1.get();
    }

    public Component joinMessageLine2() {
        return joinMessageLine2.get();
    }

    public BiConsumer<ServerPlayer, TrackingData> trackingDeltaSyncer() {
        return trackingDeltaSyncer;
    }

    public Consumer<ServerPlayer> syncOnJoin() {
        return syncOnJoin;
    }

    @ApiStatus.Experimental
    public Runnable cacheInvalidatedHook() {
        return cacheInvalidatedHook;
    }

    @ApiStatus.Experimental
    public Consumer<MinecraftServer> reloadBroadcastHook() {
        return reloadBroadcastHook;
    }

    @Nullable
    public MarieLibDataProvider dataProvider() {
        return dataProvider;
    }

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
    public ValueDefinition valueDefinitionFor(String key) {
        return ValueRegistry.get(key);
    }

    public static boolean isValueBeneficial(String valueKey) {
        ValueDefinition def = ValueRegistry.get(valueKey);
        return def == null || def.isBeneficial();
    }

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
            float score = SourceValueRegistry.getScore(itemId.toString(), def.getId());
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
            float score = SourceValueRegistry.getScore(itemId.toString(), def.getId());
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
        private Supplier<Boolean> showJoinMessage = () -> false;
        private Supplier<Component> joinMessageLine1 = Component::empty;
        private Supplier<Component> joinMessageLine2 = Component::empty;
        private BiConsumer<ServerPlayer, TrackingData> trackingDeltaSyncer = (p, d) -> {};
        private Consumer<ServerPlayer> syncOnJoin = p -> {};
        @Nullable
        private MarieLibDataProvider dataProvider;
        @Nullable
        private MarieLibRegistrationDelegate registrationDelegate;
        private Runnable cacheInvalidatedHook = () -> {};
        private Consumer<MinecraftServer> reloadBroadcastHook = server -> {};

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
        public Builder onFullTrackingDataSynced(Runnable r) { this.onFullTrackingDataSynced = r; return this; }
        @ApiStatus.Stable
        public Builder configScreenFactory(Supplier<Object> s) { this.configScreenFactory = s; return this; }
        @ApiStatus.Stable
        public Builder exportScreenFactory(Function<Object, Object> f) { this.exportScreenFactory = f; return this; }
        @ApiStatus.Stable
        public Builder importScreenFactory(Function<Object, Object> f) { this.importScreenFactory = f; return this; }
        public Builder onValuesDeltaReceived(Consumer<Map<String, Float>> c) { this.onValuesDeltaReceived = c; return this; }
        public Builder clientMemoryConfigProvider(Supplier<DiminishingReturnsConfig> s) { this.clientMemoryConfigProvider = s; return this; }
        public Builder configExporter(Supplier<JsonObject> s) { this.configExporter = s; return this; }
        public Builder configImporter(Consumer<JsonObject> c) { this.configImporter = c; return this; }
        public Builder currentConfigPresetValues(Supplier<PresetRegistry.PresetValues> s) { this.currentConfigPresetValues = s; return this; }
        public Builder ensureBuiltInPresetsOnDisk(Runnable r) { this.ensureBuiltInPresetsOnDisk = r; return this; }
        public Builder applyPresetValues(Consumer<PresetRegistry.PresetValues> c) { this.applyPresetValues = c; return this; }
        public Builder enableAllEffectsForPresets(Runnable r) { this.enableAllEffectsForPresets = r; return this; }
        @ApiStatus.Stable
        public Builder valueIconProvider(Function<String, String> f) { this.valueIconProvider = f; return this; }
        @ApiStatus.Experimental
        public Builder tooltipValueResolver(BiFunction<ItemStack, Player, Map<String, Float>> f) { this.tooltipValueResolver = f; return this; }
        public Builder clientTrackingDataProvider(Supplier<TrackingData> s) { this.clientTrackingDataProvider = s; return this; }
        public Builder sourceFamilyResolver(Function<ResourceLocation, String> f) { this.sourceFamilyResolver = f; return this; }
        @ApiStatus.Experimental
        public Builder valueTagScoresProvider(Function<Item, Map<String, Float>> f) { this.valueTagScoresProvider = f; return this; }
        @ApiStatus.Stable
        public Builder tagRoleResolver(Function<String, String> f) { this.tagRoleResolver = f; return this; }
        @ApiStatus.Stable
        public Builder heavySourceBlocker(BiPredicate<ServerPlayer, ValueSourceTrigger> p) {
            this.heavySourceBlocker = p;
            return this;
        }
        @ApiStatus.Stable
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
        @ApiStatus.Stable
        public Builder effectApplier(BiConsumer<ServerPlayer, TrackingData> c) { this.effectApplier = c; return this; }
        @ApiStatus.Stable
        public Builder effectClearer(Consumer<ServerPlayer> c) { this.effectClearer = c; return this; }
        public Builder decayIntervalTicks(Supplier<Integer> s) { this.decayIntervalTicks = s; return this; }
        @ApiStatus.Stable
        public Builder showJoinMessage(Supplier<Boolean> s) { this.showJoinMessage = s; return this; }
        @ApiStatus.Stable
        public Builder joinMessageLine1(Supplier<Component> s) { this.joinMessageLine1 = s; return this; }
        @ApiStatus.Stable
        public Builder joinMessageLine2(Supplier<Component> s) { this.joinMessageLine2 = s; return this; }
        @ApiStatus.Stable
        public Builder trackingDeltaSyncer(BiConsumer<ServerPlayer, TrackingData> c) { this.trackingDeltaSyncer = c; return this; }
        @ApiStatus.Stable
        public Builder syncOnJoin(Consumer<ServerPlayer> c) { this.syncOnJoin = c; return this; }
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
        @ApiStatus.Stable
        public Builder dataProvider(MarieLibDataProvider p) { this.dataProvider = p; return this; }
        @Deprecated
        @ApiStatus.Internal
        public Builder registrationDelegate(MarieLibRegistrationDelegate d) { this.registrationDelegate = d; return this; }

        public MarieLibContext build() {
            return new MarieLibContext(this);
        }
    }
}
