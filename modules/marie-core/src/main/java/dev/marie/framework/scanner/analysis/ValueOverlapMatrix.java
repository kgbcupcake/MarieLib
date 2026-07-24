package dev.marie.framework.scanner.analysis;

import dev.marie.framework.api.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Symmetric co-occurrence matrix of value pairs in multi-value sources.
 * Row and column keys are sorted alphabetically.
 *
 * @param matrix value → (value → co-occurrence count)
 */
@ApiStatus.Internal
public record ValueOverlapMatrix(
        Map<String, Map<String, Integer>> matrix
) {
    public ValueOverlapMatrix {
        matrix = Map.copyOf(matrix);
    }

    /**
     * Builds a sorted symmetric matrix from raw pair counts.
     * Keys in each row are sorted alphabetically; rows are sorted alphabetically.
     */
    public static ValueOverlapMatrix fromPairCounts(Map<String, Map<String, Integer>> rawCounts) {
        List<String> values = new ArrayList<>(rawCounts.keySet());
        Collections.sort(values);

        Map<String, Map<String, Integer>> sorted = new LinkedHashMap<>();
        for (String row : values) {
            Map<String, Integer> rowData = rawCounts.getOrDefault(row, Map.of());
            Map<String, Integer> sortedRow = new TreeMap<>();
            for (String col : values) {
                int count = 0;
                if (row.equals(col)) {
                    count = rowData.getOrDefault(col, 0);
                } else {
                    count = rowData.getOrDefault(col, 0);
                    if (count == 0) {
                        Map<String, Integer> reverse = rawCounts.get(col);
                        if (reverse != null) {
                            count = reverse.getOrDefault(row, 0);
                        }
                    }
                }
                sortedRow.put(col, count);
            }
            sorted.put(row, Collections.unmodifiableMap(sortedRow));
        }
        return new ValueOverlapMatrix(Collections.unmodifiableMap(sorted));
    }

    /**
     * Returns all value keys in alphabetical order.
     */
    public List<String> valueKeys() {
        return List.copyOf(matrix.keySet());
    }
}
