package dev.marie.MariesLib.compat.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.event.EventGroupWrapper;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.SourcePairSynergy;
import dev.marie.MariesLib.api.MilestoneDefinition;
import dev.marie.MariesLib.api.ProfileDefinition;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.MarieAPI;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.util.MarieValidation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * KubeJS-facing Marie API and event bindings.
 */
@ApiStatus.Internal
public final class MarieKubeJSBindings {

    public static final String API_BINDING = "MarieAPI";
    public static final String EVENTS_BINDING = "MarieEvents";

    private MarieKubeJSBindings() {}

    public static Object createBindingObject() {
        return new ScriptApi();
    }

    public static Object createEventsBindingObject(ScriptType type) {
        return new EventGroupWrapper(type, MarieKubeJSEvents.GROUP);
    }

    public static final class ScriptApi {

        public void registerValue(JsonObject data) {
            try {
                String id = data.get("id").getAsString();
                ValueDefinition.Builder builder = ValueDefinition.builder(id);
                builder.displayName(data.get("displayName").getAsString());
                if (data.has("color")) builder.color(data.get("color").getAsInt());
                if (data.has("decayRate")) builder.defaultDecayRate(data.get("decayRate").getAsFloat());
                if (data.has("critical")) builder.criticalThreshold(data.get("critical").getAsFloat());
                if (data.has("low")) builder.lowThreshold(data.get("low").getAsFloat());
                if (data.has("excess")) builder.excessThreshold(data.get("excess").getAsFloat());
                MarieAPI.registerValue(builder.build());
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register value", e);
            }
        }

        public void registerSourceClassification(String itemId, String valueKey, float amount) {
            ResourceLocation loc = ResourceLocation.parse(itemId);
            MarieValidation.requireNonNullId(loc, "registerSourceClassification");
            MarieValidation.requireFinite(amount, -10f, 10f, "registerSourceClassification.amount");
            MarieAPI.registerSourceClassification(loc, valueKey, amount);
        }

        public void registerSourcePairSynergy(String sourceA, String sourceB, int windowSeconds, String valueKey, float amount) {
            try {
                if (windowSeconds <= 0 || windowSeconds >= 3600) {
                    throw new IllegalArgumentException("windowSeconds must be in (0, 3600), got: " + windowSeconds);
                }
                MarieValidation.requireFinite(amount, -10f, 10f, "registerSourcePairSynergy.amount");
                String id = ResourceLocation.parse(sourceA).getPath() + "_" + ResourceLocation.parse(sourceB).getPath();
                SourcePairSynergy definition = SourcePairSynergy.builder(id)
                        .sourceA(ResourceLocation.parse(sourceA))
                        .sourceB(ResourceLocation.parse(sourceB))
                        .timeWindowTicks(windowSeconds * 20)
                        .bonusValueKey(valueKey)
                        .bonusAmount(amount)
                        .build();
                MarieAPI.registerSourcePairSynergy(definition);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register source synergy", e);
            }
        }

        public void registerMilestone(JsonObject data) {
            try {
                String id = data.get("id").getAsString();
                MilestoneDefinition.Builder builder = MilestoneDefinition.builder(id)
                        .valueKey(data.get("valueKey").getAsString())
                        .cumulativeGoal(data.get("target").getAsFloat());
                if (data.has("effectId")) {
                    builder.rewardEffect(ResourceLocation.parse(data.get("effectId").getAsString()));
                }
                MarieAPI.registerMilestone(builder.build());
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register milestone", e);
            }
        }

        public void registerTrackingProfile(JsonObject data) {
            try {
                String id = data.get("id").getAsString();
                ProfileDefinition.Builder builder = ProfileDefinition.builder(id)
                        .displayName(data.get("displayName").getAsString());
                if (data.has("thresholds")) {
                    for (Map.Entry<String, com.google.gson.JsonElement> e : data.getAsJsonObject("thresholds").entrySet()) {
                        builder.customThreshold(e.getKey(), e.getValue().getAsFloat());
                    }
                }
                if (data.has("decayRates")) {
                    for (Map.Entry<String, com.google.gson.JsonElement> e : data.getAsJsonObject("decayRates").entrySet()) {
                        builder.customDecayRate(e.getKey(), e.getValue().getAsFloat());
                    }
                }
                MarieAPI.registerTrackingProfile(builder.build());
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register tracking profile", e);
            }
        }

        public float getValueLevel(ServerPlayer player, String valueKey) {
            return MarieAPI.getValueLevel(player, valueKey);
        }

        public void setValueLevel(ServerPlayer player, String valueKey, float value) {
            TrackingData data = player.getData(TrackingAttachment.TRACKING.get());
            data.values.put(valueKey, Math.max(0f, Math.min(1f, value)));
            player.setData(TrackingAttachment.TRACKING.get(), data);
        }
    }
}
