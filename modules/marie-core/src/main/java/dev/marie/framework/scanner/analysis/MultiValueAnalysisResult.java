package dev.marie.framework.scanner.analysis;

import dev.marie.framework.api.ApiStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Complete result of a multi-value analysis pass.
 *
 * @param secondaryByValue Items grouped by secondary value, each list sorted score DESC then itemId ASC
 * @param ambiguousSources      Sources flagged for manual review (excluded from recommendations)
 * @param overlapMatrix       Co-occurrence counts for value pairs
 * @param metrics             Aggregate quality metrics
 */
@ApiStatus.Internal
public record MultiValueAnalysisResult(
        Map<String, List<MultiValueEntry>> secondaryByValue,
        List<AmbiguousSourceEntry> ambiguousSources,
        ValueOverlapMatrix overlapMatrix,
        ScannerMetrics metrics
) {
    private static final Comparator<MultiValueEntry> ENTRY_ORDER = Comparator
            .comparing(MultiValueEntry::score, Comparator.reverseOrder())
            .thenComparing(e -> e.itemId().toString());

    public MultiValueAnalysisResult {
        Map<String, List<MultiValueEntry>> sorted = new TreeMap<>();
        for (Map.Entry<String, List<MultiValueEntry>> entry : secondaryByValue.entrySet()) {
            List<MultiValueEntry> list = new ArrayList<>(entry.getValue());
            list.sort(ENTRY_ORDER);
            sorted.put(entry.getKey(), List.copyOf(list));
        }
        secondaryByValue = Map.copyOf(sorted);
        ambiguousSources = List.copyOf(ambiguousSources);
    }
}
