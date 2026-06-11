package dev.marie.MariesLib.config;

import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.color.ColorRegistry;

/**
 * Builds import/export JSON sections from {@link MariesLibConfigHolder}.
 */
public final class MariesLibConfigBridge {

    private MariesLibConfigBridge() {}

    public static JsonObject buildExportRoot() {
        MariesLibConfigHolder h = MariesLibConfigHolder.get();
        JsonObject root = new JsonObject();

        JsonObject general = new JsonObject();
        general.addProperty("showJoinMessage", h.showJoinMessage);
        general.addProperty("memoryWindowMinutes", h.memoryWindowMinutes);
        general.addProperty("memoryWindowCount", h.memoryWindowCount);
        root.add("general", general);

        JsonObject thresholds = new JsonObject();
        thresholds.addProperty("excessThreshold", h.excessThreshold);
        thresholds.addProperty("lowThreshold", h.lowThreshold);
        thresholds.addProperty("criticalThreshold", h.criticalThreshold);
        thresholds.addProperty("decayIntervalTicks", h.decayIntervalTicks);
        thresholds.addProperty("defaultDecayRate", h.defaultDecayRate);
        root.add("thresholds", thresholds);

        JsonObject effects = new JsonObject();
        effects.addProperty("defaultEffectDurationTicks", h.defaultEffectDurationTicks);
        effects.addProperty("enableEffects", h.enableEffects);
        root.add("effects", effects);

        JsonObject valueColors = new JsonObject();
        for (String key : ValueRegistry.getAll().stream().map(v -> v.getId()).toList()) {
            ColorRegistry.getArgb(key).ifPresent(argb ->
                    valueColors.addProperty(key, String.format("0x%08X", argb)));
        }
        root.add("valueColors", valueColors);

        JsonObject sourceValues = new JsonObject();
        root.add("sourceValues", sourceValues);

        root.add("modules", MariesLibConfigIO.holderToJson(h).getAsJsonObject("modules"));

        return root;
    }

    public static void applyImport(JsonObject root) {
        MariesLibConfigHolder h = MariesLibConfigHolder.get();

        if (root.has("general") && root.get("general").isJsonObject()) {
            JsonObject general = root.getAsJsonObject("general");
            if (general.has("showJoinMessage")) {
                h.showJoinMessage = general.get("showJoinMessage").getAsBoolean();
            }
            if (general.has("memoryWindowMinutes")) {
                h.memoryWindowMinutes = general.get("memoryWindowMinutes").getAsLong();
            }
            if (general.has("memoryWindowCount")) {
                h.memoryWindowCount = general.get("memoryWindowCount").getAsInt();
            }
        }

        if (root.has("thresholds") && root.get("thresholds").isJsonObject()) {
            JsonObject thresholds = root.getAsJsonObject("thresholds");
            if (thresholds.has("excessThreshold")) {
                h.excessThreshold = thresholds.get("excessThreshold").getAsFloat();
            }
            if (thresholds.has("lowThreshold")) {
                h.lowThreshold = thresholds.get("lowThreshold").getAsFloat();
            }
            if (thresholds.has("criticalThreshold")) {
                h.criticalThreshold = thresholds.get("criticalThreshold").getAsFloat();
            }
            if (thresholds.has("decayIntervalTicks")) {
                h.decayIntervalTicks = thresholds.get("decayIntervalTicks").getAsInt();
            }
            if (thresholds.has("defaultDecayRate")) {
                h.defaultDecayRate = thresholds.get("defaultDecayRate").getAsFloat();
            }
        }

        if (root.has("effects") && root.get("effects").isJsonObject()) {
            JsonObject effects = root.getAsJsonObject("effects");
            if (effects.has("defaultEffectDurationTicks")) {
                h.defaultEffectDurationTicks = effects.get("defaultEffectDurationTicks").getAsInt();
            }
            if (effects.has("enableEffects")) {
                h.enableEffects = effects.get("enableEffects").getAsBoolean();
            }
        }

        if (root.has("modules") && root.get("modules").isJsonObject()) {
            JsonObject wrapper = new JsonObject();
            wrapper.add("modules", root.get("modules"));
            MariesLibConfigIO.readIntoHolder(wrapper, h);
        }

        MariesLibConfigIO.save();
    }
}
