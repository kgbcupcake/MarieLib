package dev.marie.framework.handler;

import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.debug.MarieDebugLogger;
import dev.marie.framework.tracking.TrackingData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Builds and submits the per-source-application debug JSON payload for
 * {@link SourceApplicationPipeline}, extracted to keep the pipeline's orchestration free of
 * serialization detail. Owned exclusively by {@link SourceApplicationPipeline} via composition —
 * not intended to be shared or held elsewhere.
 */
@ApiStatus.Internal
final class SourceApplyDebugReporter {

    private SourceApplyDebugReporter() {}

    static void submitSourceApplyDebug(
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
            String tagPath = MarieContext.get().resolveTagRole("source_override");
            if (tagPath != null) {
                tagMatch.add(IMarieConfig.get().modId() + ":" + tagPath);
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
}
