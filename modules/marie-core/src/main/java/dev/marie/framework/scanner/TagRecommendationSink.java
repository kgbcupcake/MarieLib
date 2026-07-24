package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;

import java.io.IOException;
import java.util.List;

/**
 * Contract for writing tag recommendation reports produced by {@link ItemScanner}.
 * Implemented by marie-resources's {@code TagRecommendationWriter}, which registers itself
 * with {@link TagRecommendationSinkRegistry} — core has no compile-time dependency on
 * marie-resources, per the locked one-directional module graph.
 */
@ApiStatus.Internal
@FunctionalInterface
public interface TagRecommendationSink {
    void writeRecommendations(
            List<ClassificationResult> results,
            float spreadThreshold
    ) throws IOException;
}
