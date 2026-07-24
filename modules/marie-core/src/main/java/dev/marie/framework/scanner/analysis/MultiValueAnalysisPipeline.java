package dev.marie.framework.scanner.analysis;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.scanner.ClassificationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates multi-value analysis, aggregation, and persistence.
 *
 * <p>Analysis ({@link #analyze}) is pure and reusable by commands, GUIs, tests, and KubeJS.
 * Persistence ({@link #write}) is fault-tolerant — a failure writing one output does not
 * prevent remaining outputs from being written.</p>
 */
@ApiStatus.Internal
public final class MultiValueAnalysisPipeline {

    private MultiValueAnalysisPipeline() {}

    /**
     * Analyzes classification results for multi-value patterns.
     *
     * @param results            Classification results from the item scanner
     * @param absoluteThreshold  Minimum score for a secondary value to qualify
     * @param relativeThreshold  Secondary score must be at least dominantScore * this value
     * @param ambiguityThreshold Sources where dominant − second &lt; this are flagged ambiguous
     */
    public static MultiValueAnalysisResult analyze(
            List<ClassificationResult> results,
            float absoluteThreshold,
            float relativeThreshold,
            float ambiguityThreshold
    ) {
        Map<String, List<MultiValueEntry>> secondaryByValue = new HashMap<>();
        List<AmbiguousSourceEntry> ambiguousSources = new ArrayList<>();
        Map<String, Map<String, Integer>> rawPairCounts = new HashMap<>();

        int total = 0;
        int singleValue = 0;
        int multiValue = 0;
        int ambiguous = 0;
        int totalSecondaries = 0;

        for (ClassificationResult r : results) {
            Map<String, Float> scores = r.scores();
            if (scores == null || scores.isEmpty()) {
                continue;
            }

            total++;

            String dominant = resolveDominant(scores);
            float dominantScore = scores.getOrDefault(dominant, 0f);
            float secondScore = resolveSecondScore(scores, dominant);
            float spread = dominantScore - secondScore;

            if (!r.tagClassified() && spread < ambiguityThreshold) {
                ambiguous++;
                ambiguousSources.add(new AmbiguousSourceEntry(r.itemId(), scores, spread));
                continue;
            }

            List<String> qualifyingSecondaries = resolveQualifyingSecondaries(
                    scores, dominant, dominantScore, absoluteThreshold, relativeThreshold);

            if (qualifyingSecondaries.isEmpty()) {
                singleValue++;
            } else {
                multiValue++;
                totalSecondaries += qualifyingSecondaries.size();

                for (String secondaryKey : qualifyingSecondaries) {
                    float secondaryScore = scores.getOrDefault(secondaryKey, 0f);
                    secondaryByValue
                            .computeIfAbsent(secondaryKey, k -> new ArrayList<>())
                            .add(new MultiValueEntry(r.itemId(), secondaryScore, dominant));
                }

                Set<String> valueSet = new HashSet<>(qualifyingSecondaries);
                valueSet.add(dominant);
                incrementPairCounts(rawPairCounts, valueSet);
            }
        }

        double averageSecondaryCount = multiValue > 0
                ? (double) totalSecondaries / multiValue
                : 0.0;

        ScannerMetrics metrics = new ScannerMetrics(
                total, singleValue, multiValue, ambiguous, averageSecondaryCount);

        ValueOverlapMatrix overlapMatrix = ValueOverlapMatrix.fromPairCounts(rawPairCounts);

        return new MultiValueAnalysisResult(
                secondaryByValue, ambiguousSources, overlapMatrix, metrics);
    }

    /**
     * Writes all analysis outputs to {@code config/<MODID>/scanner_analysis/}.
     * Threshold metadata is omitted; use {@link #run} for full JSON output.
     */
    public static void write(MultiValueAnalysisResult result) throws IOException {
        Path outputDir = MultiValueAnalysisWriter.resolveOutputDir();
        Files.createDirectories(outputDir);
        MultiValueAnalysisWriter.writeAll(result, outputDir, null, null, null);
    }

    /**
     * Analyzes and writes all outputs in one step.
     */
    public static void run(
            List<ClassificationResult> results,
            float absoluteThreshold,
            float relativeThreshold,
            float ambiguityThreshold
    ) {
        MultiValueAnalysisResult result = analyze(
                results, absoluteThreshold, relativeThreshold, ambiguityThreshold);
        try {
            Path outputDir = MultiValueAnalysisWriter.resolveOutputDir();
            Files.createDirectories(outputDir);
            MultiValueAnalysisWriter.writeAll(
                    result, outputDir, absoluteThreshold, relativeThreshold, ambiguityThreshold);
        } catch (IOException e) {
            MarieCore.LOGGER.error("[MultiValueAnalysisPipeline] Failed to create output directory", e);
        }
    }

    /**
     * Analyzes a pre-collected list of classified sources and writes outputs to
     * {@code config/<MODID>/scanner_analysis/}.
     */
    public static void runFullRegistry(
            List<ClassificationResult> results,
            float absoluteThreshold,
            float relativeThreshold,
            float ambiguityThreshold
    ) {
        if (results.isEmpty()) {
            MarieCore.LOGGER.warn(
                    "[MultiValueAnalysisPipeline] Registry empty or unavailable — skipping full registry analysis");
            return;
        }

        MarieCore.LOGGER.info(
                "[MultiValueAnalysisPipeline] Pulled {} classified sources from registry",
                results.size());

        MultiValueAnalysisResult result = analyze(
                results, absoluteThreshold, relativeThreshold, ambiguityThreshold);
        try {
            write(result);
        } catch (IOException e) {
            MarieCore.LOGGER.error("[MultiValueAnalysisPipeline] Failed to write full registry analysis", e);
        }
    }

    private static String resolveDominant(Map<String, Float> scores) {
        return scores.entrySet().stream()
                .sorted(Comparator
                        .comparing(Map.Entry<String, Float>::getValue, Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
    }

    private static float resolveSecondScore(Map<String, Float> scores, String dominant) {
        return scores.entrySet().stream()
                .filter(e -> !e.getKey().equals(dominant))
                .map(Map.Entry::getValue)
                .max(Float::compare)
                .orElse(0f);
    }

    private static List<String> resolveQualifyingSecondaries(
            Map<String, Float> scores,
            String dominant,
            float dominantScore,
            float absoluteThreshold,
            float relativeThreshold
    ) {
        float relativeMin = dominantScore * relativeThreshold;
        List<String> secondaries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : scores.entrySet()) {
            String key = entry.getKey();
            float score = entry.getValue();
            if (key.equals(dominant)) {
                continue;
            }
            if (score >= absoluteThreshold && score >= relativeMin) {
                secondaries.add(key);
            }
        }
        Collections.sort(secondaries);
        return secondaries;
    }

    private static void incrementPairCounts(
            Map<String, Map<String, Integer>> rawPairCounts,
            Set<String> valueKeys
    ) {
        List<String> sorted = new ArrayList<>(valueKeys);
        Collections.sort(sorted);
        for (int i = 0; i < sorted.size(); i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                String a = sorted.get(i);
                String b = sorted.get(j);
                rawPairCounts
                        .computeIfAbsent(a, k -> new HashMap<>())
                        .merge(b, 1, Integer::sum);
                rawPairCounts
                        .computeIfAbsent(b, k -> new HashMap<>())
                        .merge(a, 1, Integer::sum);
            }
        }
    }
}
