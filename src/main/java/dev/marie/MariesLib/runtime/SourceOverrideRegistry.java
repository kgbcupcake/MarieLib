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
 * Loads per-source overrides from config/&lt;modid&gt;/source_overrides.json.
 * Escape hatch for modpack creators who want to override specific sources.
 */
@ApiStatus.Internal
public class SourceOverrideRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * An override for a specific source item.
     * @param sourceId The source ID (e.g. "minecraft:golden_apple")
     * @param values Map of value key to delta
     * @param enabled Whether this override is active
     */
    public record SourceOverride(String sourceId, Map<String, Float> values, float total, boolean enabled) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Core extends AbstractRegistry<String, SourceOverride> {
        Core() {
            super("SourceOverrideRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    public static Optional<SourceOverride> getOverride(String sourceId) {
        SourceOverride override = INSTANCE.get(sourceId);
        if (override != null && override.enabled()) {
            return Optional.of(override);
        }
        return Optional.empty();
    }

    public static SourceOverride get(String sourceId) {
        return INSTANCE.get(sourceId);
    }

    public static Map<String, SourceOverride> getAll() {
        return INSTANCE.entries();
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(IMarieLibConfig.get().modId());
        Path file = configDir.resolve("source_overrides.json");

        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                LOGGER.info("[SourceOverrideRegistry] Wrote default source_overrides.json");
            }
            parse(file);
            LOGGER.info("[SourceOverrideRegistry] Loaded {} overrides from config folder", INSTANCE.size());
        } catch (IOException e) {
            LOGGER.error("[SourceOverrideRegistry] Failed to load source_overrides.json", e);
            INSTANCE.reset();
            INSTANCE.freeze();
        }
    }

    public static void reload() {
        LOGGER.info("[SourceOverrideRegistry] Reloading source_overrides.json");
        load();
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                DatapackSchema.CONFIG_SOURCE_OVERRIDES,
                SourceOverrideRegistry::parseFromReader,
                SourceOverrideRegistry::load,
                "[SourceOverrideRegistry] Loaded from datapack override",
                "[SourceOverrideRegistry] Failed to load from datapack, falling back to config folder",
                "[SourceOverrideRegistry] Loaded from config folder"
        );
    }

    private static void parseFromReader(Reader reader) {
        INSTANCE.reset();
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        if (arr != null) {
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String sourceId = obj.get("source_id").getAsString();
                boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();

                Map<String, Float> values = new HashMap<>();
                if (obj.has("values") && obj.get("values").isJsonObject()) {
                    JsonObject valuesObj = obj.getAsJsonObject("values");
                    for (Map.Entry<String, JsonElement> entry : valuesObj.entrySet()) {
                        values.put(entry.getKey(), entry.getValue().getAsFloat());
                    }
                }

                float total = obj.has("total") && !obj.get("total").isJsonNull()
                        ? obj.get("total").getAsFloat()
                        : 0f;

                INSTANCE.register(sourceId, new SourceOverride(sourceId, values, total, enabled));
            }
        }
        INSTANCE.freeze();
    }

    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(IMarieLibConfig.get().modId());
        Path file = configDir.resolve("source_overrides.json");
        try {
            writeRegistry(file);
            LOGGER.info("[SourceOverrideRegistry] Saved source_overrides.json");
        } catch (IOException e) {
            LOGGER.error("[SourceOverrideRegistry] Failed to save source_overrides.json", e);
        }
    }

    public static void setOverride(String sourceId, Map<String, Float> values, boolean enabled) {
        Objects.requireNonNull(sourceId, "sourceId");
        LinkedHashMap<String, SourceOverride> next = new LinkedHashMap<>(INSTANCE.entries());
        next.put(sourceId, new SourceOverride(sourceId, new HashMap<>(values), 0f, enabled));
        INSTANCE.reset();
        for (Map.Entry<String, SourceOverride> e : next.entrySet()) {
            INSTANCE.register(e.getKey(), e.getValue());
        }
        INSTANCE.freeze();
    }

    public static void removeOverride(String sourceId) {
        Objects.requireNonNull(sourceId, "sourceId");
        LinkedHashMap<String, SourceOverride> next = new LinkedHashMap<>(INSTANCE.entries());
        next.remove(sourceId);
        INSTANCE.reset();
        for (Map.Entry<String, SourceOverride> e : next.entrySet()) {
            INSTANCE.register(e.getKey(), e.getValue());
        }
        INSTANCE.freeze();
    }

    private static void parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            parseFromReader(r);
        }
    }

    private static void writeDefaults(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }

    private static void writeRegistry(Path file) throws IOException {
        JsonArray arr = new JsonArray();
        for (SourceOverride override : INSTANCE.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("source_id", override.sourceId());

            JsonObject valuesObj = new JsonObject();
            for (Map.Entry<String, Float> entry : override.values().entrySet()) {
                valuesObj.addProperty(entry.getKey(), entry.getValue());
            }
            obj.add("values", valuesObj);

            obj.addProperty("total", override.total());
            obj.addProperty("enabled", override.enabled());
            arr.add(obj);
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(arr, w);
        }
    }
}
