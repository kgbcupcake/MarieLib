package dev.marie.MariesLib.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.registry.ValueRegistry;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads per-category value multipliers from config/&lt;modid&gt;/source_values.json.
 * Writes a default file on first run.
 */
@ApiStatus.Internal
public class SourceValueRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    public record SourceValueDef(String category, Map<String, Float> multipliers) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final float DEFAULT_MULTIPLIER = 0.2f;

    private static final class Core extends AbstractRegistry<String, SourceValueDef> {
        Core() {
            super("SourceValueRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    /**
     * Returns value amounts scaled by the category's multipliers.
     * Falls back to an even split across registered value keys if the category is unknown.
     */
    public static Map<String, Float> getValuesForCategory(String categoryKey, float totalPoints) {
        SourceValueDef def = INSTANCE.get(categoryKey);
        if (def == null) {
            List<String> keys = ValueRegistry.getAll().stream().map(ValueDefinition::getId).toList();
            if (keys.isEmpty()) {
                return Map.of();
            }
            Map<String, Float> fallback = new LinkedHashMap<>();
            float share = totalPoints * DEFAULT_MULTIPLIER;
            for (String key : keys) {
                fallback.put(key, share);
            }
            return fallback;
        }
        Map<String, Float> result = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : def.multipliers().entrySet()) {
            result.put(e.getKey(), totalPoints * e.getValue());
        }
        return result;
    }

    public static SourceValueDef get(String categoryKey) {
        return INSTANCE.get(categoryKey);
    }

    public static List<SourceValueDef> getAll() {
        return INSTANCE.values();
    }

    public static List<String> getCategories() {
        return INSTANCE.keys();
    }

    /**
     * Returns the registered classification score for an item/value pair, or 0 if none.
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
        Path file = configDir.resolve("source_values.json");

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                LOGGER.info("[SourceValueRegistry] Wrote default source_values.json");
            }
            parse(file);
            LOGGER.info("[SourceValueRegistry] Loaded {} categories from config folder", INSTANCE.size());
        } catch (IOException e) {
            LOGGER.error("[SourceValueRegistry] Failed to load source_values.json, using empty defaults", e);
            loadDefaults();
        }
    }

    public static void reload() {
        LOGGER.info("[SourceValueRegistry] Reloading source_values.json");
        load();
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                DatapackSchema.CONFIG_SOURCE_VALUES,
                SourceValueRegistry::parseFromReader,
                SourceValueRegistry::load,
                "[SourceValueRegistry] Loaded from datapack override",
                "[SourceValueRegistry] Failed to load from datapack, falling back to config folder",
                "[SourceValueRegistry] Loaded from config folder"
        );
    }

    private static boolean registerValueRow(JsonObject obj, int index) {
        JsonElement categoryEl = obj.get("category");
        if (categoryEl == null || !categoryEl.isJsonPrimitive() || !categoryEl.getAsJsonPrimitive().isString()) {
            LOGGER.warn("[SourceValueRegistry] Skipping entry at index {} missing or invalid 'category'", index);
            return false;
        }
        String category = categoryEl.getAsString();
        Map<String, Float> multipliers = parseMultipliers(obj.get("multipliers"));
        INSTANCE.registerUnlocked(category, new SourceValueDef(category, multipliers));
        return true;
    }

    private static Map<String, Float> parseMultipliers(JsonElement el) {
        if (el == null || !el.isJsonObject()) {
            return Map.of();
        }
        Map<String, Float> multipliers = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                multipliers.put(entry.getKey(), entry.getValue().getAsFloat());
            }
        }
        return Map.copyOf(multipliers);
    }

    private static void parseFromReader(Reader reader) {
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            if (arr == null || arr.isEmpty()) {
                LOGGER.warn("[SourceValueRegistry] Data was empty, using empty defaults");
                INSTANCE.freezeUnlocked();
                return;
            }
            for (int i = 0; i < arr.size(); i++) {
                registerValueRow(arr.get(i).getAsJsonObject(), i);
            }
            if (INSTANCE.valuesUnlocked().isEmpty()) {
                LOGGER.warn("[SourceValueRegistry] Data was empty, using empty defaults");
                INSTANCE.resetUnlocked();
            }
            INSTANCE.freezeUnlocked();
        });
    }

    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(IMarieLibConfig.get().modId());
        Path file = configDir.resolve("source_values.json");
        try {
            writeRegistry(file);
            LOGGER.info("[SourceValueRegistry] Saved source_values.json");
        } catch (IOException e) {
            LOGGER.error("[SourceValueRegistry] Failed to save source_values.json", e);
        }
    }

    public static void setCategory(String category, Map<String, Float> multipliers) {
        Objects.requireNonNull(category, "category");
        LinkedHashMap<String, SourceValueDef> next = new LinkedHashMap<>(INSTANCE.entries());
        next.put(category, new SourceValueDef(category, new HashMap<>(multipliers)));
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            for (Map.Entry<String, SourceValueDef> e : next.entrySet()) {
                INSTANCE.registerUnlocked(e.getKey(), e.getValue());
            }
            INSTANCE.freezeUnlocked();
        });
    }

    private static void parse(Path file) throws IOException {
        JsonArray arr;
        try (Reader r = Files.newBufferedReader(file)) {
            arr = GSON.fromJson(r, JsonArray.class);
        }
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            if (arr == null) {
                LOGGER.warn("[SourceValueRegistry] source_values.json was empty, using empty defaults");
                INSTANCE.freezeUnlocked();
                return;
            }
            for (int i = 0; i < arr.size(); i++) {
                registerValueRow(arr.get(i).getAsJsonObject(), i);
            }
            if (INSTANCE.valuesUnlocked().isEmpty()) {
                LOGGER.warn("[SourceValueRegistry] source_values.json was empty, using empty defaults");
                INSTANCE.resetUnlocked();
            }
            INSTANCE.freezeUnlocked();
        });
    }

    private static void loadDefaults() {
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            LOGGER.warn("[SourceValueRegistry] Using empty registry — consuming mod should provide defaults");
            INSTANCE.freezeUnlocked();
        });
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (SourceValueDef def : INSTANCE.values()) {
            arr.add(defToJson(def));
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static JsonObject defToJson(SourceValueDef def) {
        JsonObject obj = new JsonObject();
        obj.addProperty("category", def.category());
        JsonObject multipliersObj = new JsonObject();
        for (Map.Entry<String, Float> e : def.multipliers().entrySet()) {
            multipliersObj.addProperty(e.getKey(), e.getValue());
        }
        obj.add("multipliers", multipliersObj);
        return obj;
    }
}
