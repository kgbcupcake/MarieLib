package dev.marie.framework.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.data.DatapackSchema;
import dev.marie.framework.registry.AbstractRegistry;
import dev.marie.framework.util.MarieResourceLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Loads the set of globally excluded item IDs from JSON.
 *
 * <p><b>Priority / Override Stack (lowest to highest):</b></p>
 * <ol>
 *   <li>Bundled defaults at {@code data/<modid>/<modid>/scanner/excluded_items.json}</li>
 *   <li>{@code config/<modid>/excluded_items.json} (modpack creator override)</li>
 *   <li>{@code data/<ns>/<modid>/scanner/excluded_items.json} (datapack override)</li>
 * </ol>
 *
 * <p>Schema: a JSON array of item ID strings, e.g. {@code ["minecraft:sugar", "modid:candy"]}.</p>
 */
@ApiStatus.Internal
public final class ExcludedItemsRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "excluded_items.json";

    private static String bundledResourcePath() {
        return "/data/" + IMarieConfig.get().modId() + "/" + DatapackSchema.root() + "/scanner/excluded_items.json";
    }

    private static final class Core extends AbstractRegistry<String, Boolean> {
        Core() {
            super("ExcludedItemsRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private ExcludedItemsRegistry() {}

    @ApiStatus.Stable
    public static boolean isExcluded(String itemId) {
        return INSTANCE.contains(itemId);
    }

    @ApiStatus.Stable
    public static void addExcluded(String itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId cannot be null");
        }
        Set<String> current = new LinkedHashSet<>(INSTANCE.keys());
        current.add(itemId);
        replaceAll(current);
    }

    @ApiStatus.Stable
    public static void removeExcluded(String itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId cannot be null");
        }
        Set<String> current = new LinkedHashSet<>(INSTANCE.keys());
        current.remove(itemId);
        replaceAll(current);
    }

    @ApiStatus.Stable
    public static Set<String> getAll() {
        return Set.copyOf(INSTANCE.keys());
    }

    private static void replaceAll(Set<String> items) {
        INSTANCE.reset();
        for (String item : items) {
            INSTANCE.register(item, Boolean.TRUE);
        }
        INSTANCE.freeze();
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(IMarieConfig.get().modId());
        Path overridesDir = configDir.resolve("overrides");
        Path file = overridesDir.resolve(CONFIG_FILE_NAME);
        try {
            Files.createDirectories(overridesDir);
            if (!Files.exists(file)) {
                if (writeBundledTo(file)) {
                    MarieCore.LOGGER.info("[ExcludedItemsRegistry] Wrote default excluded_items.json");
                } else {
                    MarieCore.LOGGER.warn("[ExcludedItemsRegistry] No bundled excluded_items.json for this modId, skipping write");
                }
            }
            Set<String> items = parseFile(file);
            if (items == null) {
                MarieCore.LOGGER.warn("[ExcludedItemsRegistry] excluded_items.json was empty/invalid, falling back to bundled defaults");
                items = parseBundled();
            }
            if (items == null) {
                items = Set.of();
            }
            replaceAll(items);
            MarieCore.LOGGER.info("[ExcludedItemsRegistry] Loaded {} excluded items from {}", items.size(), file);
        } catch (IOException e) {
            MarieCore.LOGGER.error("[ExcludedItemsRegistry] Failed to load excluded_items.json, using bundled defaults", e);
            Set<String> bundled = parseBundled();
            replaceAll(bundled != null ? bundled : Set.of());
        }

        try {
            writeReadmeIfAbsent(overridesDir);
        } catch (IOException e) {
            MarieCore.LOGGER.warn("[ExcludedItemsRegistry] Failed to write EXCLUDED_ITEMS_README.md", e);
        }
    }

    public static void reload() {
        MarieCore.LOGGER.info("[ExcludedItemsRegistry] Reloading excluded_items.json");
        load();
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                "scanner/excluded_items.json",
                reader -> {
                    Set<String> items = parseReader(reader);
                    if (items == null) {
                        return false;
                    }
                    replaceAll(items);
                    return true;
                },
                ExcludedItemsRegistry::load,
                "[ExcludedItemsRegistry] Loaded excluded_items.json from datapack override",
                "[ExcludedItemsRegistry] Failed to load datapack override, falling back to config folder",
                null
        );
    }

    private static void writeReadmeIfAbsent(Path overridesDir) throws IOException {
        Path readme = overridesDir.resolve("EXCLUDED_ITEMS_README.md");
        if (Files.exists(readme)) {
            return;
        }
        String resourcePath = "/data/" + IMarieConfig.get().modId() + "/config/EXCLUDED_ITEMS_README.md";
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath.substring(1))) {
            if (in == null) {
                MarieCore.LOGGER.warn("[ExcludedItemsRegistry] No bundled EXCLUDED_ITEMS_README.md for this modId, skipping write. Tried resource path: {}", resourcePath);
                return;
            }
            Files.copy(in, readme);
        }
    }

    private static Set<String> parseFile(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            return parseReader(r);
        }
    }

    private static Set<String> parseBundled() {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(bundledResourcePath().substring(1))) {
            if (in == null) return null;
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parseReader(r);
            }
        } catch (IOException e) {
            MarieCore.LOGGER.error("[ExcludedItemsRegistry] Failed to read bundled excluded_items.json", e);
            return null;
        }
    }

    private static boolean writeBundledTo(Path file) throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(bundledResourcePath().substring(1))) {
            if (in == null) {
                return false;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 Writer writer = Files.newBufferedWriter(file)) {
                JsonArray arr = GSON.fromJson(reader, JsonArray.class);
                if (arr == null) {
                    throw new IOException("Bundled excluded_items.json was empty/invalid");
                }
                GSON.toJson(arr, writer);
            }
        }
        return true;
    }

    private static Set<String> parseReader(Reader reader) {
        JsonArray arr = GSON.fromJson(reader, JsonArray.class);
        if (arr == null) return null;
        Set<String> out = new LinkedHashSet<>();
        for (JsonElement el : arr) {
            if (el != null && el.isJsonPrimitive()) {
                out.add(el.getAsString());
            }
        }
        return out;
    }
}
