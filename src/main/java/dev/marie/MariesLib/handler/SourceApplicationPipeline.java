package dev.marie.MariesLib.handler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieEvents;
import dev.marie.MariesLib.api.MarieSeasonHook;
import dev.marie.MariesLib.api.AbsorptionModifier;
import dev.marie.MariesLib.api.ValueModifierEvent;
import dev.marie.MariesLib.api.registry.AbsorptionModifierRegistry;
import dev.marie.MariesLib.api.registry.SeasonHookRegistry;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.debug.MarieDebugLogger;
import dev.marie.MariesLib.runtime.SourceOverrideRegistry;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.registry.MarieAttributes;
import dev.marie.MariesLib.util.MarieItemTags;
import dev.marie.MariesLib.util.MarieRegistryUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

@ApiStatus.Internal
final class SourceApplicationPipeline {

    private static final java.util.concurrent.atomic.AtomicBoolean THRESHOLD_WARN_ONCE =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.atomic.AtomicBoolean WARN_ONCE_SOURCE_APPLIED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private SourceApplicationPipeline() {}

    static void process(ServerPlayer player, ItemStack stack, TrackingData tracking, long gameTimeMs) {
        if (ReloadHandler.isReloadInProgress()) {
            return;
        }
        var ctx = MarieLibContext.get();
        TrackingMemoryConfigOrNull config = resolveMemoryConfig();
        tracking.setMemoryConfig(config.config());
        boolean debugApplyLog = ModuleCache.enableDebugLogging;
        Map<String, Float> valuesBefore = debugApplyLog ? snapshotValues(tracking) : Map.of();

        String itemId = MarieRegistryUtils.itemKey(stack).toString();
        SourceOverrideRegistry.SourceOverride override = ctx.sourceOverrideLookup().apply(itemId);

        float totalAdded;
        Map<String, Float> valueDeltas;
        Map<String, Float> matchedBars;
        ResourceLocation sourceResourceId = MarieRegistryUtils.itemKey(stack);
        FoodProperties sourceProps = stack.getItem().getFoodProperties(stack, player);

        if (override != null) {
            totalAdded = override.total();
            valueDeltas = new HashMap<>(override.values());
            matchedBars = new HashMap<>(override.values());
            MariesLib.LOGGER.debug("[MarieLib] using override for {} (total={}, values={})",
                    itemId, totalAdded, valueDeltas);
        } else {
            if (sourceProps == null) {
                return;
            }
            matchedBars = new LinkedHashMap<>(ctx.sourceValueResolver().apply(stack, player.level()));
            MarieLibContext.SourceDelta delta = ctx.sourceDeltaResolver().resolve(
                    stack, player.level(), sourceProps.nutrition(), sourceProps.saturation(), matchedBars);
            totalAdded = delta.total();
            valueDeltas = new HashMap<>(delta.values());
        }

        Map<String, Float> matchedBarWeights = new LinkedHashMap<>(matchedBars);

        Map<String, Float> externalClassification = ctx.externalClassificationProvider().apply(sourceResourceId);
        if (externalClassification != null) {
            externalClassification.forEach((key, value) -> valueDeltas.merge(key, value, Float::sum));
        }

        String dominantCategory = matchedBars.isEmpty()
                ? null
                : matchedBars.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
        String familyKey = ctx.sourceFamilyResolver().apply(sourceResourceId);

        float multiplier = tracking.recordSource(itemId, dominantCategory, familyKey, gameTimeMs);
        TrackingData.MultiplierBreakdown multiplierBreakdown = debugApplyLog
                ? tracking.getMultiplierBreakdown(itemId, dominantCategory, familyKey, gameTimeMs)
                : null;

        if (ModuleCache.enableTotalTracking) {
            MariesLib.LOGGER.debug("[MarieLib] total: adding {} * {} for {}",
                    totalAdded, multiplier, stack.getItem().getDescriptionId());
            tracking.addTotal(totalAdded * multiplier);
        }

        Map<String, Float> afterMultiplierOnly = new HashMap<>();
        Map<String, Float> finalApplied = new HashMap<>();

        for (String key : ctx.valueKeys()) {
            float valueDelta = valueDeltas.getOrDefault(key, 0f);
            if (valueDelta != 0f) {
                float adjustedDelta = valueDelta * multiplier;
                afterMultiplierOnly.put(key, adjustedDelta);
                adjustedDelta = applySeasonalAbsorption(player, key, adjustedDelta);
                adjustedDelta = applyAbsorptionModifiers(player, key, adjustedDelta);
                adjustedDelta *= MarieAttributes.valueRegenMultiplier(player);

                ValueModifierEvent modifierEvent = new ValueModifierEvent(
                        player, sourceResourceId, key, adjustedDelta);
                NeoForge.EVENT_BUS.post(modifierEvent);

                if (modifierEvent.isCanceled()) {
                    continue;
                }

                float finalDelta = modifierEvent.getAmount();
                if (!Float.isFinite(finalDelta) || finalDelta < -10f || finalDelta > 100f) {
                    MariesLib.LOGGER.warn("[MarieLib] Invalid finalDelta {} for player={} item={} value={} — skipping",
                            finalDelta, player.getName().getString(), itemId, key);
                    continue;
                }
                finalApplied.put(key, finalDelta);
                float oldValue = tracking.values.getOrDefault(key, 0f);
                tracking.addValue(key, finalDelta);
                float newValue = tracking.values.getOrDefault(key, 0f);

                if (oldValue != newValue) {
                    NeoForge.EVENT_BUS.post(new MarieEvents.ValueChangedEvent(
                            player, key, oldValue, newValue));
                }

                NeoForge.EVENT_BUS.post(new MarieEvents.SourceAppliedEvent(
                        player, sourceResourceId, key, finalDelta));
            }
        }

        if (debugApplyLog) {
            Map<String, Float> valuesAfter = snapshotValues(tracking);
            submitSourceApplyDebug(
                    player,
                    stack,
                    gameTimeMs,
                    itemId,
                    sourceResourceId,
                    override != null,
                    matchedBarWeights,
                    valueDeltas,
                    afterMultiplierOnly,
                    finalApplied,
                    valuesBefore,
                    valuesAfter,
                    multiplier,
                    multiplierBreakdown,
                    sourceProps
            );
        }

        checkThresholdCrossings(player, tracking);

        player.setData(TrackingAttachment.TRACKING.get(), tracking);
        ctx.trackingDeltaSyncer().accept(player, tracking);
        ctx.effectApplier().accept(player, tracking);

        MariesLib.LOGGER.debug("{} applied {} -> {}",
                player.getName().getString(),
                stack.getItem().getDescriptionId(),
                tracking);
    }

    private static TrackingMemoryConfigOrNull resolveMemoryConfig() {
        var provider = MarieLibContext.get().trackingMemoryConfigProvider();
        if (provider.get() != null) {
            return new TrackingMemoryConfigOrNull(provider.get(), false);
        }
        if (WARN_ONCE_SOURCE_APPLIED.compareAndSet(false, true)) {
            MariesLib.LOGGER.warn(
                    "[MarieLib] SourceApplicationPipeline: trackingMemoryConfigProvider returned null, falling back to context defaults. Will not warn again until server restart.");
        }
        return new TrackingMemoryConfigOrNull(HandlerSupport.resolveMemoryConfig(), true);
    }

    private static Map<String, Float> snapshotValues(TrackingData tracking) {
        Map<String, Float> m = new HashMap<>();
        for (String key : MarieLibContext.get().valueKeys()) {
            m.put(key, tracking.values.getOrDefault(key, 0f));
        }
        return m;
    }

    private static void submitSourceApplyDebug(
            ServerPlayer player,
            ItemStack stack,
            long gameTimeMs,
            String itemIdStr,
            ResourceLocation sourceResourceId,
            boolean sourceOverride,
            Map<String, Float> matchedBarWeights,
            Map<String, Float> rawValueDelta,
            Map<String, Float> afterMultiplierOnly,
            Map<String, Float> finalApplied,
            Map<String, Float> valuesBefore,
            Map<String, Float> valuesAfter,
            float multiplier,
            TrackingData.MultiplierBreakdown breakdown,
            FoodProperties source
    ) {
        JsonObject root = new JsonObject();
        root.addProperty("timestamp", MarieDebugLogger.isoTimestamp());
        root.addProperty("classifier_path", sourceOverride ? "SOURCE_OVERRIDE" : "RESOLVER");
        root.addProperty("pipeline_stage", sourceOverride ? "SOURCE_OVERRIDE" : "RESOLVER");

        JsonObject playerJo = new JsonObject();
        playerJo.addProperty("name", player.getName().getString());
        playerJo.addProperty("uuid", player.getUUID().toString());
        root.add("player", playerJo);

        JsonObject itemJo = new JsonObject();
        itemJo.addProperty("id", itemIdStr);
        itemJo.addProperty("namespace", sourceResourceId.getNamespace());
        root.add("item", itemJo);

        JsonArray tagMatch = new JsonArray();
        if (sourceOverride) {
            tagMatch.add(MarieLibContext.get().modId() + ":source_override");
        } else if (matchedBarWeights.isEmpty()) {
            tagMatch.add("none");
        } else {
            matchedBarWeights.keySet().forEach(tagMatch::add);
        }
        root.add("tag_match", tagMatch);

        root.add("classifier_signals", new JsonArray());
        root.add("matched_bars", MarieDebugLogger.floatMapToJson(matchedBarWeights));
        root.add("recipe_inheritance", new JsonArray());
        root.add("raw_value_delta", MarieDebugLogger.floatMapToJson(rawValueDelta));
        root.addProperty("multiplier", MarieDebugLogger.round4(multiplier));
        root.add("multiplier_breakdown", buildMultiplierBreakdownJson(breakdown));
        root.add("after_multiplier_value_delta", MarieDebugLogger.floatMapToJson(afterMultiplierOnly));
        root.add("final_value_delta", MarieDebugLogger.floatMapToJson(finalApplied));
        root.add("values_before", MarieDebugLogger.floatMapToJson(valuesBefore));
        root.add("values_after", MarieDebugLogger.floatMapToJson(valuesAfter));

        boolean lightBypass = source != null
                && (source.nutrition() <= 2 || stack.is(MarieItemTags.lightSource()))
                && !stack.is(MarieItemTags.heavySource());
        root.addProperty("light_snack_bypass_eligible", lightBypass);

        root.addProperty("game_time_ms", gameTimeMs);
        root.addProperty("game_time_ticks", player.level().getGameTime());
        root.addProperty("dimension", player.level().dimension().location().toString());

        MarieDebugLogger.submitSourceLog(root);
    }

    private static JsonObject buildMultiplierBreakdownJson(TrackingData.MultiplierBreakdown b) {
        JsonObject o = new JsonObject();
        if (b == null) {
            return o;
        }
        float fin = b.finalMultiplier();
        o.addProperty("item_contribution", MarieDebugLogger.round4(b.itemContribution()));
        o.addProperty("category_contribution", MarieDebugLogger.round4(b.categoryContribution()));
        o.addProperty("family_contribution", MarieDebugLogger.round4(b.familyContribution()));
        o.addProperty("novelty_contribution", MarieDebugLogger.round4(b.noveltyContribution()));
        o.addProperty("final_multiplier", MarieDebugLogger.round4(fin));
        o.addProperty("item_weight", MarieDebugLogger.round4(b.itemWeight()));
        o.addProperty("category_weight", MarieDebugLogger.round4(b.categoryWeight()));
        o.addProperty("family_weight", MarieDebugLogger.round4(b.familyWeight()));
        o.addProperty("item_percent_of_final", MarieDebugLogger.pctOfFinal(b.itemContribution(), fin));
        o.addProperty("category_percent_of_final", MarieDebugLogger.pctOfFinal(b.categoryContribution(), fin));
        o.addProperty("family_percent_of_final", MarieDebugLogger.pctOfFinal(b.familyContribution(), fin));
        return o;
    }

    private static void checkThresholdCrossings(ServerPlayer player, TrackingData tracking) {
        float excessThreshold = MarieLibContext.get().excessThreshold();
        if (MarieLibContext.get().trackingMemoryConfigProvider().get() == null
                && THRESHOLD_WARN_ONCE.compareAndSet(false, true)) {
            MariesLib.LOGGER.warn(
                    "[MarieLib] SourceApplicationPipeline: trackingMemoryConfigProvider null in checkThresholdCrossings, using context excessThreshold. Will not warn again until server restart.");
        }
        for (String key : MarieLibContext.get().valueKeys()) {
            float current = tracking.values.getOrDefault(key, 0f);
            float previous = tracking.lastValues.getOrDefault(key, 0f);
            boolean beneficial = MarieLibContext.get().isValueBeneficial().test(key);
            float criticalThreshold = MarieLibContext.get().criticalThresholdFor(key);

            if (beneficial) {
                if (current <= criticalThreshold && previous > criticalThreshold) {
                    NeoForge.EVENT_BUS.post(new MarieEvents.ValueCriticalEvent(player, key));
                }
                if (current >= excessThreshold && previous < excessThreshold) {
                    NeoForge.EVENT_BUS.post(new MarieEvents.ValueExcessEvent(player, key));
                }
            } else {
                if (current >= excessThreshold && previous < excessThreshold) {
                    NeoForge.EVENT_BUS.post(new MarieEvents.ValueCriticalEvent(player, key));
                }
            }
        }
    }

    private static float applySeasonalAbsorption(ServerPlayer player, String valueKey, float baseAmount) {
        var hooks = SeasonHookRegistry.getAll();
        if (!ModuleCache.enableSeasonHooks || hooks.isEmpty()) {
            return baseAmount;
        }
        float amount = baseAmount;
        for (MarieSeasonHook hook : hooks) {
            float mult = hook.getSeasonalAbsorptionModifier(valueKey, MarieSeasonHook.Season.SPRING);
            if (!Float.isFinite(mult)) {
                MariesLib.LOGGER.warn("[MarieLib] Seasonal modifier returned non-finite value {} for value={} — using 0", mult, valueKey);
                mult = 0f;
            }
            amount *= Math.max(0f, mult);
        }
        if (!Float.isFinite(amount)) {
            MariesLib.LOGGER.warn("[MarieLib] Seasonal absorption result non-finite for value={} — using 0", valueKey);
            return 0f;
        }
        return amount;
    }

    private static float applyAbsorptionModifiers(ServerPlayer player, String valueKey, float baseAmount) {
        var modifiers = AbsorptionModifierRegistry.getAll();
        if (!ModuleCache.enableAbsorptionModifiers || modifiers.isEmpty()) {
            return baseAmount;
        }
        float amount = baseAmount;
        for (AbsorptionModifier modifier : modifiers) {
            float factor = modifier.getAbsorptionMultiplier(player, valueKey, amount);
            if (!Float.isFinite(factor)) {
                MariesLib.LOGGER.warn("[MarieLib] Absorption modifier returned non-finite value {} for value={} — using 0", factor, valueKey);
                factor = 0f;
            }
            amount *= Math.max(0f, factor);
        }
        if (!Float.isFinite(amount)) {
            MariesLib.LOGGER.warn("[MarieLib] Absorption modifier result non-finite for value={} — using 0", valueKey);
            return 0f;
        }
        return amount;
    }

    static void resetSnapshotWarnings() {
        WARN_ONCE_SOURCE_APPLIED.set(false);
        THRESHOLD_WARN_ONCE.set(false);
    }

    private record TrackingMemoryConfigOrNull(
            dev.marie.MariesLib.tracking.TrackingMemoryConfig config,
            boolean wasNullProvider
    ) {}
}
