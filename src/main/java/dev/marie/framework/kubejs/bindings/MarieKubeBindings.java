package dev.marie.MariesLib.kubejs.bindings;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPI;
import dev.marie.MariesLib.api.MilestoneDefinition;
import dev.marie.MariesLib.api.SourcePairSynergy;
import dev.marie.MariesLib.api.SynergyDefinition;
import dev.marie.MariesLib.api.ThresholdEffect;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.runtime.SourceClassificationRegistry;
import dev.marie.MariesLib.util.MarieValidation;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Startup script bindings for value and registration helpers.
 */
@ApiStatus.Experimental
public final class MarieKubeBindings {

    private MarieKubeBindings() {}

    public static void registerValue(Map<String, Object> spec) {
        String id = requireString(spec, "id");
        ValueDefinition.Builder builder = ValueDefinition.builder(id);
        builder.displayName(requireString(spec, "displayName"));
        if (spec.containsKey("color")) {
            builder.color(asInt(spec.get("color")));
        }
        if (spec.containsKey("decayRate")) {
            builder.defaultDecayRate(asFloat(spec.get("decayRate")));
        }
        if (spec.containsKey("critical")) {
            builder.criticalThreshold(asFloat(spec.get("critical")));
        }
        if (spec.containsKey("low")) {
            builder.lowThreshold(asFloat(spec.get("low")));
        }
        if (spec.containsKey("excess")) {
            builder.excessThreshold(asFloat(spec.get("excess")));
        }
        if (spec.containsKey("amountScale")) {
            builder.amountScale(asDouble(spec.get("amountScale")));
        }
        if (spec.containsKey("beneficial")) {
            builder.beneficial(asBoolean(spec.get("beneficial")));
        }
        MarieAPI.registerValue(builder.build());
    }

    public static void registerSourceClassification(String itemId, String valueKey, float amount) {
        ResourceLocation loc = ResourceLocation.parse(itemId);
        MarieValidation.requireNonNullId(loc, "registerSourceClassification");
        MarieValidation.requireFiniteUnbounded(amount, "registerSourceClassification.amount");
        MarieAPI.registerSourceClassification(loc, valueKey, amount);
    }

    public static void registerSourceOverride(String itemId, Map<String, Object> overrides) {
        Map<String, Float> values = new HashMap<>();
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            if ("enabled".equals(entry.getKey())) {
                continue;
            }
            values.put(entry.getKey(), asFloat(entry.getValue()));
        }
        boolean enabled = !overrides.containsKey("enabled") || asBoolean(overrides.get("enabled"));
        SourceClassificationRegistry.setOverride(itemId, values, enabled);
    }

    public static void registerEffect(Map<String, Object> spec) {
        ThresholdEffect.Builder builder = ThresholdEffect.builder()
                .valueKey(requireString(spec, "valueKey"))
                .thresholdType(ThresholdEffect.ThresholdType.valueOf(
                        requireString(spec, "thresholdType").toUpperCase()))
                .effectId(ResourceLocation.parse(requireString(spec, "effectId")));
        if (spec.containsKey("threshold")) {
            builder.threshold(asFloat(spec.get("threshold")));
        }
        if (spec.containsKey("amplifier")) {
            builder.amplifier(asInt(spec.get("amplifier")));
        }
        if (spec.containsKey("duration")) {
            builder.duration(asInt(spec.get("duration")));
        }
        MarieAPI.registerCustomEffect(builder.build());
    }

    public static void registerMilestone(Map<String, Object> spec) {
        float goal = spec.containsKey("target")
                ? asFloat(spec.get("target"))
                : asFloat(spec.get("cumulativeGoal"));
        MilestoneDefinition.Builder builder = MilestoneDefinition.builder(requireString(spec, "id"))
                .valueKey(requireString(spec, "valueKey"))
                .cumulativeGoal(goal);
        if (spec.containsKey("effectId")) {
            builder.rewardEffect(ResourceLocation.parse(requireString(spec, "effectId")));
        }
        if (spec.containsKey("rewardAmplifier")) {
            builder.rewardAmplifier(asInt(spec.get("rewardAmplifier")));
        }
        if (spec.containsKey("rewardDuration")) {
            builder.rewardDuration(asInt(spec.get("rewardDuration")));
        }
        if (spec.containsKey("advancementId")) {
            builder.advancement(ResourceLocation.parse(requireString(spec, "advancementId")));
        }
        MarieAPI.registerMilestone(builder.build());
    }

    public static void registerValueSynergy(Map<String, Object> spec) {
        SynergyDefinition.Builder builder = SynergyDefinition.builder(requireString(spec, "id"))
                .valueA(requireString(spec, "valueKeyA"),
                        SynergyDefinition.LevelCondition.valueOf(
                                requireString(spec, "conditionA").toUpperCase()))
                .valueB(requireString(spec, "valueKeyB"),
                        SynergyDefinition.LevelCondition.valueOf(
                                requireString(spec, "conditionB").toUpperCase()));
        if (spec.containsKey("effectId")) {
            builder.bonusEffect(ResourceLocation.parse(requireString(spec, "effectId")));
        }
        if (spec.containsKey("effectAmplifier")) {
            builder.effectAmplifier(asInt(spec.get("effectAmplifier")));
        }
        if (spec.containsKey("effectDuration")) {
            builder.effectDuration(asInt(spec.get("effectDuration")));
        }
        if (spec.containsKey("penalty")) {
            builder.penalty(asBoolean(spec.get("penalty")));
        }
        MarieAPI.registerValueSynergy(builder.build());
    }

    public static void registerSourcePairSynergy(
            String sourceA,
            String sourceB,
            int windowSeconds,
            String valueKey,
            float amount
    ) {
        if (windowSeconds <= 0 || windowSeconds >= 3600) {
            throw new IllegalArgumentException("windowSeconds must be in (0, 3600), got: " + windowSeconds);
        }
        MarieValidation.requireFiniteUnbounded(amount, "registerSourcePairSynergy.amount");
        String id = ResourceLocation.parse(sourceA).getPath() + "_" + ResourceLocation.parse(sourceB).getPath();
        SourcePairSynergy definition = SourcePairSynergy.builder(id)
                .sourceA(ResourceLocation.parse(sourceA))
                .sourceB(ResourceLocation.parse(sourceB))
                .timeWindowTicks(windowSeconds * 20)
                .bonusValueKey(valueKey)
                .bonusAmount(amount)
                .build();
        MarieAPI.registerSourcePairSynergy(definition);
    }

    private static String requireString(Map<String, Object> spec, String key) {
        Object value = spec.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return String.valueOf(value);
    }

    private static float asFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(value));
    }

    private static double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
