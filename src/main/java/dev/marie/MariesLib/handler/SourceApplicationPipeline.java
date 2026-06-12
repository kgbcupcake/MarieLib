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
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.ValueModifierContext;
import dev.marie.MariesLib.api.ValueModifierEvent;
import dev.marie.MariesLib.api.ValueSourceTrigger;
import dev.marie.MariesLib.api.registry.AbsorptionModifierRegistry;
import dev.marie.MariesLib.api.registry.SeasonHookRegistry;
import dev.marie.MariesLib.config.FeatureFlagCache;
import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.debug.MarieDebugLogger;
import dev.marie.MariesLib.runtime.SourceOverrideRegistry;
import dev.marie.MariesLib.runtime.SourceTriggerRegistry;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.registry.MarieAttributes;
import dev.marie.MariesLib.core.KubeIntegration;
import dev.marie.MariesLib.util.MarieRegistryUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

@ApiStatus.Internal
public final class SourceApplicationPipeline {

    private static final java.util.concurrent.atomic.AtomicBoolean THRESHOLD_WARN_ONCE =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.atomic.AtomicBoolean WARN_ONCE_SOURCE_APPLIED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private SourceApplicationPipeline() {}

    public static void process(ServerPlayer player, ValueSourceTrigger trigger, ItemStack stack,
                             TrackingData tracking, long gameTimeMs) {
        if (ReloadGuardListener.isReloadInProgress()) {
            return;
        }

        var ctx = MarieLibContext.get();
        MariesLib.LOGGER.info("[DEBUG] Pipeline firing for {} — resolver returned: {}",
                stack != null ? stack.getItem().getDescriptionId() : "null",
                stack != null && !stack.isEmpty()
                        ? ctx.sourceValueResolver().apply(stack, player.level())
                        : Map.of());

        MarieEvents.SourceTriggerEvent triggerEvent =
                new MarieEvents.SourceTriggerEvent(player, trigger);
        NeoForge.EVENT_BUS.post(triggerEvent);
        if (triggerEvent.isCanceled()) {
            return;
        }

        if (FeatureFlagCache.enableBlockHeavySources()
                && ctx.isHeavySourceBlocked(player, trigger)) {
            return;
        }
        if (FeatureFlagCache.enableBlockLightSource()
                && ctx.isLightSourceBlocked(player, trigger)) {
            return;
        }
        DiminishingReturnsConfigOrNull config = resolveMemoryConfig();
        tracking.setMemoryConfig(config.config());
        boolean debugApplyLog = FeatureFlagCache.enableDebugLogging();
        Map<String, Float> valuesBefore = debugApplyLog ? snapshotValues(tracking) : Map.of();

        String sourceKey = trigger.sourceId();
        SourceOverrideRegistry.SourceOverride override =
                SourceOverrideRegistry.getOverride(sourceKey).orElse(null);

        float totalAdded;
        Map<String, Float> valueDeltas;
        Map<String, Float> matchedBars;
        ResourceLocation sourceResourceId = stack != null
                ? MarieRegistryUtils.itemKey(stack)
                : ResourceLocation.parse(trigger.sourceId());
        if (override != null) {
            totalAdded = override.total();
            valueDeltas = new HashMap<>(override.values());
            matchedBars = new HashMap<>(override.values());
            MariesLib.LOGGER.debug("[MarieLib] using override for {} (total={}, values={})",
                    sourceKey, totalAdded, valueDeltas);
        } else if (stack == null || stack.isEmpty()) {
            matchedBars = Map.of();
            MarieLibContext.SourceDelta delta = ctx.sourceDeltaResolver().resolve(
                    stack, player.level(), trigger.payload(), matchedBars);
            totalAdded = delta.total();
            valueDeltas = new HashMap<>(delta.values());
        } else {
            matchedBars = new LinkedHashMap<>(ctx.sourceValueResolver().apply(stack, player.level()));
            MarieLibContext.SourceDelta delta = ctx.sourceDeltaResolver().resolve(
                    stack, player.level(), trigger.payload(), matchedBars);
            totalAdded = delta.total();
            valueDeltas = new HashMap<>(delta.values());
        }

        Map<String, Float> matchedBarWeights = new LinkedHashMap<>(matchedBars);

        java.util.List<SourceTriggerRegistry.Entry> triggerEntries =
                SourceTriggerRegistry.getEntries(trigger.type(), trigger.sourceId());
        for (SourceTriggerRegistry.Entry entry : triggerEntries) {
            valueDeltas.merge(entry.valueKey(), entry.amount(), Float::sum);
        }

        String dominantCategory = matchedBars.isEmpty()
                ? null
                : matchedBars.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
        String familyKey = ctx.sourceFamilyResolver().apply(sourceResourceId);

        float multiplier = tracking.recordSource(sourceKey, dominantCategory, familyKey, gameTimeMs);
        TrackingData.MultiplierBreakdown multiplierBreakdown = debugApplyLog
                ? tracking.getMultiplierBreakdown(sourceKey, dominantCategory, familyKey, gameTimeMs)
                : null;

        if (FeatureFlagCache.enableTotalTracking()) {
            MariesLib.LOGGER.debug("[MarieLib] total: adding {} * {} for {}",
                    totalAdded, multiplier,
                    stack != null ? stack.getItem().getDescriptionId() : trigger.sourceId());
            tracking.addTotal(totalAdded * multiplier);
        }

        Map<String, Float> afterMultiplierOnly = new HashMap<>();
        Map<String, Float> finalApplied = new HashMap<>();

        for (String key : ctx.valueKeys()) {
            float valueDelta = valueDeltas.getOrDefault(key, 0f);
            ValueDefinition valueDef = MarieLibContext.get().valueDefinitionFor(key);
            if (valueDef != null && valueDef.getAmountScale() != 1.0) {
                valueDelta = (float) (valueDelta / valueDef.getAmountScale());
            }
            if (valueDelta != 0f) {
                float adjustedDelta = valueDelta * multiplier;
                afterMultiplierOnly.put(key, adjustedDelta);
                adjustedDelta = applySeasonalAbsorption(player, key, adjustedDelta);
                adjustedDelta = applyAbsorptionModifiers(player, key, adjustedDelta);
                adjustedDelta *= MarieAttributes.valueRegenMultiplier(player);
                ValueModifierContext modifierCtx =
                        ValueModifierContext.of(player, sourceResourceId, key);
                adjustedDelta = KubeIntegration.applyValueDeltaModifier(modifierCtx, adjustedDelta);

                ValueModifierEvent modifierEvent = new ValueModifierEvent(modifierCtx, adjustedDelta);
                NeoForge.EVENT_BUS.post(modifierEvent);

                if (modifierEvent.isCanceled()) {
                    continue;
                }

                float finalDelta = modifierEvent.getAmount();
                finalDelta = MarieLibContext.get().applyPostValueModifier(modifierCtx, finalDelta);
                if (!Float.isFinite(finalDelta)) {
                    MariesLib.LOGGER.warn("[MarieLib] non-finite finalDelta {} for player={} source={} value={} — skipping",
                            finalDelta, player.getName().getString(), sourceKey, key);
                    continue;
                }
                if (finalDelta == 0f) {
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
                    sourceKey,
                    sourceResourceId,
                    override != null,
                    matchedBarWeights,
                    valueDeltas,
                    afterMultiplierOnly,
                    finalApplied,
                    valuesBefore,
                    valuesAfter,
                    multiplier,
                    multiplierBreakdown
            );
        }

        checkThresholdCrossings(player, tracking);

        TrackingAttachment.setData(player, tracking);
        ctx.trackingDeltaSyncer().accept(player, tracking);
        ctx.effectApplier().accept(player, tracking);

        MariesLib.LOGGER.debug("{} applied {} -> {}",
                player.getName().getString(),
                stack != null ? stack.getItem().getDescriptionId() : trigger.sourceId(),
                tracking);
    }

    private static DiminishingReturnsConfigOrNull resolveMemoryConfig() {
        return new DiminishingReturnsConfigOrNull(IMarieLibConfig.get().trackingMemoryConfig(), false);
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
            TrackingData.MultiplierBreakdown breakdown
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
            String tagPath = MarieLibContext.get().resolveTagRole("source_override");
            if (tagPath != null) {
                tagMatch.add(IMarieLibConfig.get().modId() + ":" + tagPath);
            }
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
        float excessThreshold = IMarieLibConfig.get().excessThreshold();
        for (String key : MarieLibContext.get().valueKeys()) {
            float current = tracking.values.getOrDefault(key, 0f);
            float previous = tracking.lastValues.getOrDefault(key, 0f);
            boolean beneficial = MarieLibContext.isValueBeneficial(key);
            float criticalThreshold = IMarieLibConfig.get().criticalThresholdFor(key);

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
        if (!FeatureFlagCache.enableSeasonHooks() || hooks.isEmpty()) {
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
        if (!FeatureFlagCache.enableAbsorptionModifiers() || modifiers.isEmpty()) {
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

    private record DiminishingReturnsConfigOrNull(
            dev.marie.MariesLib.tracking.DiminishingReturnsConfig config,
            boolean wasNullProvider
    ) {}
}
