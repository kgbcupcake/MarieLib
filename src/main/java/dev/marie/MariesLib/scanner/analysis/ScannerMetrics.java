package dev.marie.MariesLib.scanner.analysis;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * Aggregate quality metrics for a multi-value analysis run.
 *
 * @param total                  Total sources analyzed
 * @param singleValue         Sources with only a dominant value (no qualifying secondaries)
 * @param multiValue          Sources with at least one qualifying secondary value
 * @param ambiguous              Sources flagged as ambiguous (dominant/second spread too small)
 * @param averageSecondaryCount  Average qualifying secondary count per multi-value source
 */
@ApiStatus.Internal
public record ScannerMetrics(
        int total,
        int singleValue,
        int multiValue,
        int ambiguous,
        double averageSecondaryCount
) {}
