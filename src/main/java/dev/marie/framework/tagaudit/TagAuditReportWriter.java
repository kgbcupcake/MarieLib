package dev.marie.framework.tagaudit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MariesLib;
import dev.marie.framework.tagaudit.model.TagFixSuggestion;
import dev.marie.framework.tagaudit.model.TagIssue;
import dev.marie.framework.tagaudit.model.TagReport;
import dev.marie.framework.util.MarieValidation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes a {@link TagReport} to {@code config/<modId>/tag_audit_report.json}.
 */
@ApiStatus.Internal
public final class TagAuditReportWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TagAuditReportWriter() {}

    /**
     * @param modId  config subdirectory name (typically the consuming mod's modId)
     * @param report scan output to serialize
     * @return the path written, or null on failure
     */
    public static Path write(String modId, TagReport report) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(modId);
        Path file = configDir.resolve("tag_audit_report.json");
        try {
            Files.createDirectories(configDir);
            MarieValidation.assertPathUnder(file, configDir, "TagAuditReportWriter.write");

            JsonObject root = new JsonObject();
            root.addProperty("modId", modId);
            root.addProperty("timestamp", report.timestamp());
            root.addProperty("rulesRun", String.join(", ", report.rulesRun()));

            JsonArray issuesArr = new JsonArray();
            for (TagIssue issue : report.issues()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("issueId", issue.issueId());
                obj.addProperty("itemId", issue.itemId().toString());
                obj.addProperty("currentTag", issue.currentTag());
                obj.addProperty("ruleId", issue.ruleId());
                obj.addProperty("confidence", issue.confidence());
                obj.addProperty("severity", issue.severity().name());
                obj.addProperty("reason", issue.reason());
                issuesArr.add(obj);
            }
            root.add("issues", issuesArr);

            JsonArray suggestionsArr = new JsonArray();
            for (TagFixSuggestion suggestion : report.suggestions()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("issueId", suggestion.issueId());
                obj.addProperty("suggestedTag", suggestion.suggestedTag());
                obj.addProperty("ruleId", suggestion.ruleId());
                obj.addProperty("confidence", suggestion.confidence());
                obj.addProperty("reason", suggestion.reason());
                suggestionsArr.add(obj);
            }
            root.add("suggestions", suggestionsArr);

            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(root, w);
            }
            MariesLib.LOGGER.info("[TagAuditReportWriter] Wrote tag audit report ({} issues, {} suggestions) to {}",
                    report.issues().size(), report.suggestions().size(), file);
            return file;
        } catch (IOException e) {
            MariesLib.LOGGER.error("[TagAuditReportWriter] Failed to write tag audit report for '{}'", modId, e);
            return null;
        }
    }
}
