package dev.marie.MariesLib.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * Blend helper that merges tag-derived and runtime-resolver-derived value maps,
 * returning both the result and detailed trace information about discards and precedence.
 */
@ApiStatus.Internal
public final class ValueBlend {

    /**
     * Precedence outcome for a single value key in the blend.
     */
    public enum Precedence {
        TAG,
        RUNTIME_SUPPLEMENT
    }

    /**
     * Immutable blend outcome with trace detail.
     */
    public record BlendOutcome(
            Map<String, Float> result,
            Map<String, Float> discardedResolver,
            Map<String, Precedence> perKeyPrecedence
    ) {
        public BlendOutcome {
            result = Map.copyOf(result);
            discardedResolver = Map.copyOf(discardedResolver);
            perKeyPrecedence = Map.copyOf(perKeyPrecedence);
        }
    }

    private ValueBlend() {}

    /**
     * Blends tag-derived and resolver-derived value maps.
     *
     * <p>Tags are authoritative: they seed the result at full weight. The resolver may only
     * add values <em>not</em> already keyed by tags, at {@code 0.5x} weight before normalization.
     * If both inputs have the same value key, the resolver contribution is <strong>discarded</strong>.</p>
     *
     * @param tagMatches tag-derived value map (compat-filtered)
     * @param resolved   runtime resolver value map
     * @return blend outcome with result, discarded keys, and per-key precedence
     */
    public static BlendOutcome blend(Map<String, Float> tagMatches, Map<String, Float> resolved) {
        Map<String, Float> merged = new LinkedHashMap<>(tagMatches);
        Map<String, Float> discarded = new TreeMap<>();
        Map<String, Precedence> precedence = new TreeMap<>();

        for (String k : tagMatches.keySet()) {
            precedence.put(k, Precedence.TAG);
        }

        resolved.forEach((k, v) -> {
            if (tagMatches.containsKey(k)) {
                discarded.put(k, v);
            } else {
                merged.merge(k, v * 0.5f, Float::sum);
                precedence.put(k, Precedence.RUNTIME_SUPPLEMENT);
            }
        });

        float total = 0f;
        for (float v : merged.values()) total += v;

        if (total <= 0f) {
            return new BlendOutcome(tagMatches, discarded, precedence);
        }

        final float norm = total;
        Map<String, Float> result = new LinkedHashMap<>(merged.size());
        merged.forEach((k, v) -> result.put(k, v / norm));

        return new BlendOutcome(result, discarded, precedence);
    }
}
