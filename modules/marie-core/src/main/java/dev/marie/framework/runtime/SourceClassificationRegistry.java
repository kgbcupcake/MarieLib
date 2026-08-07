package dev.marie.framework.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.data.DatapackSchema;
import dev.marie.framework.registry.AbstractRegistry;
import dev.marie.framework.util.MarieResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Loads per-source manual value assignments from config/&lt;modid&gt;/source_classifications.json.
 * Replaces both SourceOverrideRegistry (source_overrides.json) and SourceValueRegistry (source_values.json).
 * On first load, migrates existing old files automatically.
 */
@ApiStatus.Internal
public class SourceClassificationRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    public record SourceClassification(String sourceId, Map<String, Float> values, float total, boolean enabled) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, SourceClassification> {
        Core() {
            super("SourceClassificationRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    // sourceIds pushed into SourceRegistry by the last pushToSourceRegistry() call, so the next call can drop stale ones first.
    private static Set<ResourceLocation> bridgedSourceIds = Set.of();

    public static Optional<SourceClassification> getOverride(String sourceId) {
        SourceClassification entry = INSTANCE.get(sourceId);
        if (entry != null && entry.enabled()) {
            return Optional.of(entry);
        }
        return Optional.empty();
    }

    public static SourceClassification get(String sourceId) {
        return INSTANCE.get(sourceId);
    }

    public static Map<String, SourceClassification> getAll() {
        return INSTANCE.entries();
    }

    /**
     * Returns the registered classification score for an item/value pair, or 0 if none.
     * Delegates to SourceRegistry for scanner-derived external classifications.
     */
    public static float getScore(String itemId, String valueKey) {
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            return 0f;
        }
        Map<String, Float> classification = SourceRegistry.getExternalClassification(loc);
        if (classification == null) {
            return 0f;
        }
        Float score = classification.get(valueKey);
        return score != null ? score : 0f;
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(IMarieConfig.get().modId());
        Path overridesDir = configDir.resolve("overrides");
        Path dataDir = overridesDir.resolve("Overrides");
        Path readmeDir = overridesDir.resolve("Read_Me");
        Path newFile = dataDir.resolve("source_classifications.json");
        Path oldFlatFile = overridesDir.resolve("source_classifications.json");
        Path oldOverrides = configDir.resolve("source_overrides.json");
        Path oldValues = configDir.resolve("source_values.json");
        Path oldRootFile = configDir.resolve("source_classifications.json");

        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(readmeDir);
            if (Files.exists(oldFlatFile) && !Files.exists(newFile)) {
                Files.move(oldFlatFile, newFile);
            }
            if (Files.exists(newFile)) {
                parse(newFile);
                LOGGER.info("[SourceClassificationRegistry] Loaded {} entries from config folder", INSTANCE.size());
                if (Files.deleteIfExists(oldOverrides) | Files.deleteIfExists(oldValues) | Files.deleteIfExists(oldFlatFile) | Files.deleteIfExists(oldRootFile)) {
                    LOGGER.info("[SourceClassificationRegistry] Cleaned up leftover legacy files (source_overrides.json / source_values.json / flat source_classifications.json / root source_classifications.json)");
                }
            } else if (Files.exists(oldOverrides) || Files.exists(oldValues) || hasContent(oldRootFile)) {
                migrateFromLegacy(newFile, oldOverrides, oldValues, oldRootFile);
                Files.deleteIfExists(oldOverrides);
                Files.deleteIfExists(oldValues);
                Files.deleteIfExists(oldRootFile);
                LOGGER.warn("[SourceClassificationRegistry] Migrated source_values.json, source_overrides.json, and root source_classifications.json into source_classifications.json. The old files were deleted.");
            } else {
                Files.deleteIfExists(oldRootFile);
                writeDefaults(newFile);
                LOGGER.info("[SourceClassificationRegistry] Wrote default source_classifications.json");
                parse(newFile);
            }
        } catch (IOException e) {
            LOGGER.error("[SourceClassificationRegistry] Failed to load source_classifications.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
            pushToSourceRegistry();
        } catch (RuntimeException e) {
            // Per-entry parse failures are isolated in parseEntry()/parseFromReader(); this only
            // catches whole-file corruption (invalid JSON syntax, or top-level value isn't an array).
            LOGGER.error("[SourceClassificationRegistry] source_classifications.json is not valid JSON, ignoring file", e);
            INSTANCE.reset();
            INSTANCE.freeze();
            pushToSourceRegistry();
        }

        try {
            Path oldReadme = overridesDir.resolve("SOURCE_CLASSIFICATIONS_README.md");
            if (Files.exists(oldReadme)) {
                Files.deleteIfExists(oldReadme);
            }
            writeReadmeIfAbsent(readmeDir);
        } catch (IOException e) {
            LOGGER.warn("[SourceClassificationRegistry] Failed to write SOURCE_CLASSIFICATIONS_README.md", e);
        }
    }

    public static void reload() {
        LOGGER.info("[SourceClassificationRegistry] Reloading source_classifications.json");
        load();
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                DatapackSchema.CONFIG_SOURCE_CLASSIFICATIONS,
                SourceClassificationRegistry::parseFromReader,
                SourceClassificationRegistry::load,
                "[SourceClassificationRegistry] Loaded from datapack override",
                "[SourceClassificationRegistry] Failed to load from datapack, falling back to config folder",
                "[SourceClassificationRegistry] Loaded from config folder"
        );
    }

    private static void parseFromReader(Reader reader) {
        INSTANCE.reset();
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                JsonElement el = arr.get(i);
                if (!el.isJsonObject()) {
                    LOGGER.warn("[SourceClassificationRegistry] Skipping malformed entry at index {}: not a JSON object", i);
                    continue;
                }
                parseEntry(el.getAsJsonObject(), i);
            }
        }
        INSTANCE.freeze();
        pushToSourceRegistry();
    }

    // Bridges enabled INSTANCE entries into SourceRegistry so getScore()/getExternalClassification() see them.
    private static void pushToSourceRegistry() {
        for (ResourceLocation staleId : bridgedSourceIds) {
            SourceRegistry.unregisterClassification(staleId);
        }
        Set<ResourceLocation> pushed = new HashSet<>();
        for (SourceClassification entry : INSTANCE.entries().values()) {
            if (!entry.enabled() || entry.values().isEmpty()) {
                continue;
            }
            ResourceLocation loc = ResourceLocation.tryParse(entry.sourceId());
            if (loc == null) {
                LOGGER.warn("[SourceClassificationRegistry] Skipping entry with malformed source_id: {}", entry.sourceId());
                continue;
            }
            // Isolated like parseEntry(): one bad entry must not abort the whole reload pass and skip every registry queued after this one.
            try {
                SourceRegistry.applyAuthoritativeOverride(loc, entry.values());
                pushed.add(loc);
            } catch (RuntimeException e) {
                LOGGER.warn("[SourceClassificationRegistry] Failed to push override for {}: {}", entry.sourceId(), e.getMessage());
            }
        }
        bridgedSourceIds = pushed;
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            parseFromReader(r);
        }
    }

    private static void parseEntry(JsonObject obj, int index) {
        try {
            if (!obj.has("source_id")) {
                LOGGER.warn("[SourceClassificationRegistry] Skipping malformed entry at index {}: missing \"source_id\"", index);
                return;
            }
            String sourceId = obj.get("source_id").getAsString();
            boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();

            Map<String, Float> values = new HashMap<>();
            if (obj.has("values") && obj.get("values").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("values").entrySet()) {
                    values.put(entry.getKey(), entry.getValue().getAsFloat());
                }
            }

            float total = obj.has("total") && !obj.get("total").isJsonNull()
                    ? obj.get("total").getAsFloat()
                    : 0f;

            INSTANCE.register(sourceId, new SourceClassification(sourceId, values, total, enabled));
        } catch (RuntimeException e) {
            JsonElement idEl = obj.get("source_id");
            String label = (idEl != null && idEl.isJsonPrimitive()) ? idEl.getAsString() : ("index " + index);
            LOGGER.warn("[SourceClassificationRegistry] Skipping malformed entry ({}): {}", label, e.getMessage());
        }
    }

    private static void migrateFromLegacy(Path newFile, Path oldOverrides, Path oldValues, Path oldRootFile) throws IOException {
        JsonArray merged = new JsonArray();

        if (Files.exists(oldOverrides)) {
            try (Reader r = Files.newBufferedReader(oldOverrides)) {
                JsonArray arr = GSON.fromJson(r, JsonArray.class);
                if (arr != null) {
                    for (JsonElement el : arr) {
                        merged.add(el);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[SourceClassificationRegistry] Could not read source_overrides.json during migration: {}", e.getMessage());
            }
        }

        if (Files.exists(oldRootFile)) {
            try (Reader r = Files.newBufferedReader(oldRootFile)) {
                JsonArray arr = GSON.fromJson(r, JsonArray.class);
                if (arr != null) {
                    for (JsonElement el : arr) {
                        merged.add(el);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[SourceClassificationRegistry] Could not read root source_classifications.json during migration: {}", e.getMessage());
            }
        }

        try (Writer w = Files.newBufferedWriter(newFile)) {
            GSON.toJson(merged, w);
        }

        INSTANCE.reset();
        for (int i = 0; i < merged.size(); i++) {
            JsonElement el = merged.get(i);
            if (!el.isJsonObject()) {
                LOGGER.warn("[SourceClassificationRegistry] Skipping malformed entry at index {}: not a JSON object", i);
                continue;
            }
            parseEntry(el.getAsJsonObject(), i);
        }
        INSTANCE.freeze();
        pushToSourceRegistry();
        LOGGER.info("[SourceClassificationRegistry] Migration complete — {} entries written to source_classifications.json", INSTANCE.size());
    }

    private static boolean hasContent(Path file) {
        if (!Files.exists(file)) {
            return false;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            JsonArray arr = GSON.fromJson(r, JsonArray.class);
            return arr != null && !arr.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static void writeDefaults(Path file) throws IOException {
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(new JsonArray(), w);
        }
    }

    private static void writeReadmeIfAbsent(Path overridesDir) throws IOException {
        Path readme = overridesDir.resolve("SOURCE_CLASSIFICATIONS_README.md");
        if (Files.exists(readme)) {
            return;
        }
        String resourcePath = "/data/" + IMarieConfig.get().modId() + "/config/SOURCE_CLASSIFICATIONS_README.md";
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath.substring(1))) {
            if (in == null) {
                LOGGER.warn("[SourceClassificationRegistry] No bundled SOURCE_CLASSIFICATIONS_README.md for this modId, skipping write. Tried resource path: {}", resourcePath);
                return;
            }
            Files.copy(in, readme);
        }
    }

    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(IMarieConfig.get().modId());
        Path file = configDir.resolve("overrides").resolve("Overrides").resolve("source_classifications.json");
        try {
            writeRegistry(file);
            LOGGER.info("[SourceClassificationRegistry] Saved source_classifications.json");
        } catch (IOException e) {
            LOGGER.error("[SourceClassificationRegistry] Failed to save source_classifications.json", e);
        }
    }

    public static void setOverride(String sourceId, Map<String, Float> values, boolean enabled) {
        Objects.requireNonNull(sourceId, "sourceId");
        LinkedHashMap<String, SourceClassification> next = new LinkedHashMap<>(INSTANCE.entries());
        next.put(sourceId, new SourceClassification(sourceId, new HashMap<>(values), 0f, enabled));
        INSTANCE.reset();
        for (Map.Entry<String, SourceClassification> e : next.entrySet()) {
            INSTANCE.register(e.getKey(), e.getValue());
        }
        INSTANCE.freeze();
        pushToSourceRegistry();
    }

    public static void removeOverride(String sourceId) {
        Objects.requireNonNull(sourceId, "sourceId");
        LinkedHashMap<String, SourceClassification> next = new LinkedHashMap<>(INSTANCE.entries());
        next.remove(sourceId);
        INSTANCE.reset();
        for (Map.Entry<String, SourceClassification> e : next.entrySet()) {
            INSTANCE.register(e.getKey(), e.getValue());
        }
        INSTANCE.freeze();
        pushToSourceRegistry();
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (SourceClassification entry : INSTANCE.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("source_id", entry.sourceId());

            JsonObject valuesObj = new JsonObject();
            for (Map.Entry<String, Float> e : entry.values().entrySet()) {
                valuesObj.addProperty(e.getKey(), e.getValue());
            }
            obj.add("values", valuesObj);

            obj.addProperty("total", entry.total());
            obj.addProperty("enabled", entry.enabled());
            arr.add(obj);
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
