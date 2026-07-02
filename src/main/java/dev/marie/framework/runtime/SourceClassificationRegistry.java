package dev.marie.MariesLib.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.marie.MariesLib.data.DatapackSchema;
import dev.marie.MariesLib.registry.AbstractRegistry;
import dev.marie.MariesLib.util.MarieResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Loads per-source manual nutrient assignments from config/&lt;modid&gt;/source_classifications.json.
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
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(IMarieLibConfig.get().modId());
        Path newFile = configDir.resolve("source_classifications.json");
        Path oldOverrides = configDir.resolve("source_overrides.json");
        Path oldValues = configDir.resolve("source_values.json");

        try {
            Files.createDirectories(configDir);
            if (Files.exists(newFile)) {
                parse(newFile);
                LOGGER.info("[SourceClassificationRegistry] Loaded {} entries from config folder", INSTANCE.size());
            } else if (Files.exists(oldOverrides) || Files.exists(oldValues)) {
                migrateFromLegacy(newFile, oldOverrides, oldValues);
                LOGGER.warn("[SourceClassificationRegistry] Migrated source_values.json and source_overrides.json into source_classifications.json. You can delete the old files.");
            } else {
                writeDefaults(newFile);
                LOGGER.info("[SourceClassificationRegistry] Wrote default source_classifications.json");
                parse(newFile);
            }
        } catch (IOException e) {
            LOGGER.error("[SourceClassificationRegistry] Failed to load source_classifications.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
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
            for (JsonElement el : arr) {
                parseEntry(el.getAsJsonObject());
            }
        }
        INSTANCE.freeze();
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            parseFromReader(r);
        }
    }

    private static void parseEntry(JsonObject obj) {
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
    }

    private static void migrateFromLegacy(Path newFile, Path oldOverrides, Path oldValues) throws IOException {
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

        try (Writer w = Files.newBufferedWriter(newFile)) {
            GSON.toJson(merged, w);
        }

        INSTANCE.reset();
        for (JsonElement el : merged) {
            parseEntry(el.getAsJsonObject());
        }
        INSTANCE.freeze();
        LOGGER.info("[SourceClassificationRegistry] Migration complete — {} entries written to source_classifications.json", INSTANCE.size());
    }

    private static void writeDefaults(Path file) throws IOException {
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(new JsonArray(), w);
        }
    }

    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(IMarieLibConfig.get().modId());
        Path file = configDir.resolve("source_classifications.json");
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
