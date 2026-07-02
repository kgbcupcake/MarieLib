package dev.marie.framework.scanner.analysis;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * A source item whose dominant and second-highest value scores are too close
 * to classify confidently. Flagged for manual review.
 *
 * @param itemId The item's registry ID
 * @param scores All value scores from classification
 * @param spread Difference between dominant and second-highest scores
 */
@ApiStatus.Internal
public record AmbiguousSourceEntry(
        ResourceLocation itemId,
        Map<String, Float> scores,
        float spread
) {
    public AmbiguousSourceEntry {
        scores = Map.copyOf(scores);
    }
}
