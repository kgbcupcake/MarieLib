package dev.marie.framework.tooltips;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Player/modpack-dev-editable tooltip color overrides, keyed explicitly by {@code modId}.
 * <p>
 * Mirrors {@link TooltipMessageRegistry}'s tiering: every lookup takes {@code modId}
 * as an explicit parameter, so it works for any consumer mod regardless of whether that mod ever
 * registers a {@code MarieContext}.
 * <p>
 * <b>Priority / Override Stack (lowest to highest):</b>
 * <ol>
 *   <li>No override (caller falls back to its own default)</li>
 *   <li>{@code config/<modId>/tooltips/tooltip_colors.json} (modpack creator override)</li>
 *   <li>{@code data/<modId>/marie/tooltips/tooltip_colors.json} (datapack override)</li>
 * </ol>
 * Both tiers share the same JSON shape:
 * <pre>{@code
 * {
 *   "byKey": { "<key>": "<ARGB hex>", ... },
 *   "byItem": { "<itemId>": "<ARGB hex>", ... }
 * }
 * }</pre>
 * {@code byItem} entries take priority over {@code byKey} for {@link #getForItem}, per tier.
 */
public final class TooltipColorRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "tooltip_colors.json";
    private static final String DATAPACK_RELATIVE_PATH = "marie/tooltips/tooltip_colors.json";

    private static final Map<String, OverrideTiers> CONFIG_CACHE = new HashMap<>();
    private static final Map<String, OverrideTiers> DATAPACK_CACHE = new HashMap<>();

    private TooltipColorRegistry() {}

    @ApiStatus.Experimental
    public static Optional<Integer> get(String modId, String key) {
        OverrideTiers datapack = DATAPACK_CACHE.get(modId);
        Optional<Integer> fromDatapack = lookupArgb(datapack == null ? null : datapack.byKey(), key, modId, key);
        if (fromDatapack.isPresent()) {
            return fromDatapack;
        }
        OverrideTiers config = CONFIG_CACHE.computeIfAbsent(modId, TooltipColorRegistry::loadConfigTier);
        return lookupArgb(config.byKey(), key, modId, key);
    }

    @ApiStatus.Experimental
    public static Optional<Integer> getForItem(String modId, String itemId, String key) {
        OverrideTiers datapack = DATAPACK_CACHE.get(modId);
        Optional<Integer> fromDatapackItem = lookupArgb(datapack == null ? null : datapack.byItem(), itemId, modId, key);
        if (fromDatapackItem.isPresent()) {
            return fromDatapackItem;
        }
        OverrideTiers config = CONFIG_CACHE.computeIfAbsent(modId, TooltipColorRegistry::loadConfigTier);
        Optional<Integer> fromConfigItem = lookupArgb(config.byItem(), itemId, modId, key);
        if (fromConfigItem.isPresent()) {
            return fromConfigItem;
        }
        return get(modId, key);
    }

    /**
     * Writes {@code config/<modId>/tooltips/tooltip_colors.json} seeded with {@code byKeyDefaults}
     * if that file does not already exist. Never overwrites an existing file. Not called automatically
     * by this class — consumer mods must invoke this explicitly at their own startup with their own
     * default values, since this class has no domain knowledge of what those defaults should be.
     */
    public static void seedDefaultsIfAbsent(String modId, Map<String, String> byKeyDefaults) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(modId).resolve("tooltips");
        Path file = configDir.resolve(CONFIG_FILE_NAME);
        if (Files.exists(file)) {
            return;
        }
        try {
            Files.createDirectories(configDir);
            OverrideTiers seeded = new OverrideTiers(new HashMap<>(byKeyDefaults), new HashMap<>());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(seeded, writer);
            }
        } catch (IOException e) {
            MarieCore.LOGGER.warn("[TooltipColorRegistry] Failed to seed tooltip_colors.json for {}", modId, e);
        }
    }

    public static void reload(String modId) {
        CONFIG_CACHE.remove(modId);
    }

    public static void loadFromDatapack(String modId, ResourceManager resourceManager) {
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(modId, DATAPACK_RELATIVE_PATH);
        Optional<Resource> resource = resourceManager.getResource(path);
        if (resource.isEmpty()) {
            DATAPACK_CACHE.remove(modId);
            return;
        }
        try (InputStream is = resource.get().open();
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            OverrideTiers tiers = GSON.fromJson(reader, OverrideTiers.class);
            if (tiers != null) {
                DATAPACK_CACHE.put(modId, tiers.normalized());
            } else {
                DATAPACK_CACHE.remove(modId);
            }
        } catch (IOException e) {
            DATAPACK_CACHE.remove(modId);
        }
    }

    private static Optional<Integer> lookupArgb(Map<String, String> colors, String key, String diagModId, String diagKey) {
        if (colors == null) {
            return Optional.empty();
        }
        String raw = colors.get(key);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return parseArgb(raw, diagModId, diagKey);
    }

    private static OverrideTiers loadConfigTier(String modId) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(modId).resolve("tooltips");
        Path file = configDir.resolve(CONFIG_FILE_NAME);
        OverrideTiers result;
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeEmpty(file);
            }
            result = parse(file);
        } catch (IOException e) {
            result = OverrideTiers.empty();
        }

        try {
            writeReadmeIfAbsent(configDir, modId);
        } catch (IOException e) {
            MarieCore.LOGGER.warn("[TooltipColorRegistry] Failed to write TOOLTIP_COLORS_README.md", e);
        }

        return result;
    }

    private static void writeReadmeIfAbsent(Path configDir, String modId) throws IOException {
        Path readme = configDir.resolve("TOOLTIP_COLORS_README.md");
        if (Files.exists(readme)) {
            return;
        }
        String resourcePath = "/data/" + modId + "/config/TOOLTIP_COLORS_README.md";
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath.substring(1))) {
            if (in == null) {
                MarieCore.LOGGER.warn("[TooltipColorRegistry] No bundled TOOLTIP_COLORS_README.md for this modId, skipping write. Tried resource path: {}", resourcePath);
                return;
            }
            Files.copy(in, readme);
        }
    }

    private static OverrideTiers parse(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            OverrideTiers tiers = GSON.fromJson(reader, OverrideTiers.class);
            return tiers != null ? tiers.normalized() : OverrideTiers.empty();
        }
    }

    private static void writeEmpty(Path file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(OverrideTiers.empty(), writer);
        }
    }

    private static Optional<Integer> parseArgb(String raw, String diagModId, String diagKey) {
        String s = raw.trim();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                s = s.substring(2);
            } else if (s.startsWith("#")) {
                s = s.substring(1);
            }
            if (s.length() == 6) {
                return Optional.of((int) (Long.parseLong(s, 16) & 0xFF_FF_FF) | 0xFF00_0000);
            }
            if (s.length() == 8) {
                return Optional.of((int) (Long.parseLong(s, 16) & 0xFF_FF_FF_FFL));
            }
        } catch (NumberFormatException ignored) {
        }
        return Optional.empty();
    }

    private record OverrideTiers(Map<String, String> byKey, Map<String, String> byItem) {
        static OverrideTiers empty() {
            return new OverrideTiers(Map.of(), Map.of());
        }

        OverrideTiers normalized() {
            return new OverrideTiers(
                    byKey != null ? byKey : Map.of(),
                    byItem != null ? byItem : Map.of());
        }
    }
}
