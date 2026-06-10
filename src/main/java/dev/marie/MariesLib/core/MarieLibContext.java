package dev.marie.MariesLib.core;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.tracking.TrackingMemoryConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Runtime context injected by the consuming mod at bootstrap.
 * Holds the mod ID and config-derived values that MarieLib cannot own.
 */
@ApiStatus.Stable
public final class MarieLibContext {

    private static volatile MarieLibContext instance;

    private final String modId;
    private final Supplier<Float> scannerConfidenceSpreadThreshold;
    private final Supplier<Float> compositeRatioThreshold;
    private final Supplier<Boolean> scannerEnableRecipeInheritance;
    private final Supplier<Boolean> enableDebugLogging;
    private final Supplier<List<String>> valueKeys;
    private final Consumer<Map<ResourceLocation, Map<String, Float>>> scannerApplyCallback;
    private final Supplier<Predicate<ItemStack>> valueTagChecker;
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
    @Nullable
    public MarieLibPlayerDataProvider playerDataProvider() { return playerDataProvider; }
    @Nullable
    public MarieLibRegistrationDelegate registrationDelegate() { return registrationDelegate; }

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
        public Builder playerDataProvider(MarieLibPlayerDataProvider p) { this.playerDataProvider = p; return this; }
        public Builder registrationDelegate(MarieLibRegistrationDelegate d) { this.registrationDelegate = d; return this; }

        public MarieLibContext build() { return new MarieLibContext(this); }
    }
}
