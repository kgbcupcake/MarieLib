package dev.marie.framework.scanner.analysis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MariesLib;
import dev.marie.framework.core.MarieLibContext;
import dev.marie.framework.util.MarieValidation;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists {@link MultiValueAnalysisResult} outputs under {@code config/<MODID>/scanner_analysis/}.
 */
@ApiStatus.Internal
final class MultiValueAnalysisWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String OUTPUT_SUBDIR = "scanner_analysis";
    private static final String DATAPACK_SUBDIR = "generated-multi-value-datapack";

    private MultiValueAnalysisWriter() {}

    static Path resolveOutputDir() {
        return FMLPaths.CONFIGDIR.get().resolve(MarieLibContext.get().modId()).resolve(OUTPUT_SUBDIR);
    }

    static void writeAll(
            MultiValueAnalysisResult result,
            Path outputDir,
            @Nullable Float absoluteThreshold,
            @Nullable Float relativeThreshold,
            @Nullable Float ambiguityThreshold
    ) {
        writeSafely(outputDir.resolve("multi_value_recommendations.json"), () ->
                writeRecommendationsJson(result, outputDir.resolve("multi_value_recommendations.json"),
                        absoluteThreshold, relativeThreshold, ambiguityThreshold));

        writeSafely(outputDir.resolve("multi_value_recommendations.txt"), () ->
                writeRecommendationsTxt(result, outputDir.resolve("multi_value_recommendations.txt")));

        writeSafely(outputDir.resolve("ambiguous_sources.txt"), () ->
                writeAmbiguousSourcesTxt(result, outputDir.resolve("ambiguous_sources.txt")));

        writeSafely(outputDir.resolve("value_overlap_matrix.txt"), () ->
                writeOverlapMatrixTxt(result, outputDir.resolve("value_overlap_matrix.txt")));

        writeSafely(outputDir.resolve("scanner_metrics.txt"), () ->
                writeMetricsTxt(result, outputDir.resolve("scanner_metrics.txt")));

        writeSafely(outputDir.resolve(DATAPACK_SUBDIR), () ->
                writeDatapack(result, outputDir.resolve(DATAPACK_SUBDIR)));

        ScannerMetrics m = result.metrics();
        MariesLib.LOGGER.info(
                "[MultiValueAnalysisPipeline] Wrote analysis: {} sources, {} multi-value, {} ambiguous → {}",
                m.total(), m.multiValue(), m.ambiguous(), outputDir.toAbsolutePath());
    }

    private static void writeSafely(Path target, WriteAction action) {
        try {
            action.run();
        } catch (IOException e) {
            MariesLib.LOGGER.error(
                    "[MultiValueAnalysisPipeline] Failed to write {}", target.getFileName(), e);
        }
    }

    @FunctionalInterface
    private interface WriteAction {
        void run() throws IOException;
    }

    private static void writeRecommendationsJson(
            MultiValueAnalysisResult result,
            Path outputFile,
            @Nullable Float absoluteThreshold,
            @Nullable Float relativeThreshold,
            @Nullable Float ambiguityThreshold
    ) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("generated", LocalDateTime.now().format(TIMESTAMP_FORMAT));

        if (absoluteThreshold != null && relativeThreshold != null && ambiguityThreshold != null) {
            JsonObject thresholds = new JsonObject();
            thresholds.addProperty("absolute", absoluteThreshold);
            thresholds.addProperty("relative", relativeThreshold);
            thresholds.addProperty("ambiguity", ambiguityThreshold);
            root.add("thresholds", thresholds);
        }

        root.addProperty("note", "Secondary value tag recommendations for multi-value sources. "
                + "Copy entries to data/" + MarieLibContext.get().modId() + "/tags/item/values/<category>.json");

        JsonObject categories = new JsonObject();
        for (Map.Entry<String, List<MultiValueEntry>> entry : result.secondaryByValue().entrySet()) {
            JsonObject categoryObj = new JsonObject();
            categoryObj.addProperty("target_file",
                    "data/" + MarieLibContext.get().modId() + "/tags/item/values/" + entry.getKey() + ".json");

            JsonArray entries = new JsonArray();
            for (MultiValueEntry e : entry.getValue()) {
                JsonObject entryObj = new JsonObject();
                entryObj.addProperty("id", e.itemId().toString());
                entryObj.addProperty("score", e.score());
                entryObj.addProperty("dominant", e.dominant());
                entries.add(entryObj);
            }
            categoryObj.add("entries", entries);
            categories.add(entry.getKey(), categoryObj);
        }
        root.add("categories", categories);

        JsonObject summary = buildSummaryJson(result.metrics());
        root.add("summary", summary);

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            GSON.toJson(root, writer);
        }
    }

    private static JsonObject buildSummaryJson(ScannerMetrics metrics) {
        JsonObject summary = new JsonObject();
        summary.addProperty("total", metrics.total());
        summary.addProperty("single_value", metrics.singleValue());
        summary.addProperty("multi_value", metrics.multiValue());
        summary.addProperty("ambiguous", metrics.ambiguous());
        summary.addProperty("average_secondary_count", metrics.averageSecondaryCount());
        return summary;
    }

    private static void writeRecommendationsTxt(
            MultiValueAnalysisResult result,
            Path outputFile
    ) throws IOException {
        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                  MULTI-VALUE TAG RECOMMENDATIONS\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n\n");
            writer.write("Instructions:\n");
            writer.write("  Add items below to their secondary value tag file:\n");
            writer.write("  data/" + MarieLibContext.get().modId() + "/tags/item/values/<category>.json\n\n");

            if (result.secondaryByValue().isEmpty()) {
                writer.write("  (no multi-value recommendations)\n");
            }

            for (Map.Entry<String, List<MultiValueEntry>> entry : result.secondaryByValue().entrySet()) {
                String value = entry.getKey();
                List<MultiValueEntry> items = entry.getValue();

                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write("  SECONDARY: " + value.toUpperCase() + " (" + items.size() + " items)\n");
                writer.write("  Target: data/" + MarieLibContext.get().modId() + "/tags/item/values/" + value + ".json\n");
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n\n");

                for (MultiValueEntry e : items) {
                    writer.write(String.format("    %s  score=%.3f  dominant=%s\n",
                            e.itemId(), e.score(), e.dominant()));
                }
                writer.write("\n");
            }

            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                           QUICK COPY BLOCKS\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");

            for (Map.Entry<String, List<MultiValueEntry>> entry : result.secondaryByValue().entrySet()) {
                String value = entry.getKey();
                List<MultiValueEntry> items = entry.getValue();

                writer.write("--- " + value + ".json ---\n");
                writer.write("{\n");
                writer.write("  \"replace\": false,\n");
                writer.write("  \"values\": [\n");

                for (int i = 0; i < items.size(); i++) {
                    MultiValueEntry e = items.get(i);
                    String comma = (i < items.size() - 1) ? "," : "";
                    writer.write(String.format("    {\"id\": \"%s\", \"required\": false}%s\n",
                            e.itemId(), comma));
                }

                writer.write("  ]\n");
                writer.write("}\n\n");
            }
        }
    }

    private static void writeAmbiguousSourcesTxt(
            MultiValueAnalysisResult result,
            Path outputFile
    ) throws IOException {
        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                     AMBIGUOUS SOURCES — MANUAL REVIEW\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
            writer.write("Count: " + result.ambiguousSources().size() + "\n\n");

            if (result.ambiguousSources().isEmpty()) {
                writer.write("  (no ambiguous sources)\n");
                return;
            }

            for (AmbiguousSourceEntry entry : result.ambiguousSources()) {
                writer.write("─────────────────────────────────────────────────────────────────────────────────\n");
                writer.write("  " + entry.itemId() + "\n");
                writer.write("  spread=" + String.format("%.3f", entry.spread()) + "\n");
                writer.write("  *** MANUAL REVIEW ***\n\n");
                writer.write("  Scores:\n");

                List<Map.Entry<String, Float>> sorted = entry.scores().entrySet().stream()
                        .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                        .toList();
                for (Map.Entry<String, Float> score : sorted) {
                    writer.write(String.format("    %-12s %.3f\n", score.getKey(), score.getValue()));
                }
                writer.write("\n");
            }
        }
    }

    private static void writeOverlapMatrixTxt(
            MultiValueAnalysisResult result,
            Path outputFile
    ) throws IOException {
        ValueOverlapMatrix matrix = result.overlapMatrix();
        List<String> valueKeys = matrix.valueKeys();

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                     VALUE OVERLAP MATRIX\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
            writer.write("Co-occurrence counts for value pairs in multi-value sources.\n\n");

            if (valueKeys.isEmpty()) {
                writer.write("  (no co-occurrence data)\n");
                return;
            }

            int colWidth = Math.max(10, valueKeys.stream().mapToInt(String::length).max().orElse(8) + 2);
            String headerFormat = "%-" + colWidth + "s";

            writer.write(String.format(headerFormat, ""));
            for (String col : valueKeys) {
                writer.write(String.format(headerFormat, col));
            }
            writer.write("\n");

            for (String row : valueKeys) {
                writer.write(String.format(headerFormat, row));
                Map<String, Integer> rowData = matrix.matrix().getOrDefault(row, Map.of());
                for (String col : valueKeys) {
                    writer.write(String.format(headerFormat, rowData.getOrDefault(col, 0)));
                }
                writer.write("\n");
            }
        }
    }

    private static void writeMetricsTxt(
            MultiValueAnalysisResult result,
            Path outputFile
    ) throws IOException {
        ScannerMetrics m = result.metrics();

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n");
            writer.write("                     SCANNER ANALYSIS METRICS\n");
            writer.write("═══════════════════════════════════════════════════════════════════════════════\n\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n\n");
            writer.write(String.format("  Total sources analyzed:       %d\n", m.total()));
            writer.write(String.format("  Single-value:            %d\n", m.singleValue()));
            writer.write(String.format("  Multi-value:             %d\n", m.multiValue()));
            writer.write(String.format("  Ambiguous (manual review):  %d\n", m.ambiguous()));
            writer.write(String.format("  Avg secondary count:        %.2f\n", m.averageSecondaryCount()));
            writer.write("\n");

            if (m.total() > 0) {
                double multiPct = 100.0 * m.multiValue() / m.total();
                double ambPct = 100.0 * m.ambiguous() / m.total();
                writer.write(String.format("  Multi-value rate:        %.1f%%\n", multiPct));
                writer.write(String.format("  Ambiguity rate:             %.1f%%\n", ambPct));
            }
        }
    }

    private static void writeDatapack(
            MultiValueAnalysisResult result,
            Path datapackRoot
    ) throws IOException {
        Files.createDirectories(datapackRoot);

        Path packMeta = datapackRoot.resolve("pack.mcmeta");
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 48);
        pack.addProperty("description", MarieLibContext.get().modId() + " auto-generated multi-value tags");
        root.add("pack", pack);
        try (Writer w = Files.newBufferedWriter(packMeta)) {
            GSON.toJson(root, w);
        }

        Path tagsDir = datapackRoot
                .resolve("data")
                .resolve(MarieLibContext.get().modId())
                .resolve("tags")
                .resolve("item")
                .resolve("values");
        Files.createDirectories(tagsDir);

        for (Map.Entry<String, List<MultiValueEntry>> entry : result.secondaryByValue().entrySet()) {
            String value = entry.getKey();
            List<MultiValueEntry> items = entry.getValue();

            Map<String, MultiValueEntry> uniqueById = new LinkedHashMap<>();
            for (MultiValueEntry e : items) {
                uniqueById.putIfAbsent(e.itemId().toString(), e);
            }

            Path tagFile = tagsDir.resolve(value + ".json");
            MarieValidation.assertPathUnder(tagFile, tagsDir, "MultiValueAnalysisWriter.writeDatapack");
            JsonObject tagObj = new JsonObject();
            tagObj.addProperty("replace", false);
            JsonArray values = new JsonArray();
            List<String> sortedIds = new ArrayList<>(uniqueById.keySet());
            Collections.sort(sortedIds);
            for (String id : sortedIds) {
                JsonObject val = new JsonObject();
                val.addProperty("id", id);
                val.addProperty("required", false);
                values.add(val);
            }
            tagObj.add("values", values);

            try (Writer w = Files.newBufferedWriter(tagFile)) {
                GSON.toJson(tagObj, w);
            }
        }
    }
}
