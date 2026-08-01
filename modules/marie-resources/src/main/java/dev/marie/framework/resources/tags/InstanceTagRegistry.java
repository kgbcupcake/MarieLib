package dev.marie.framework.resources.tags;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.core.MarieModRegistry;
import dev.marie.framework.scanner.InstanceTagSource;
import dev.marie.framework.util.MarieValidation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Instance-level (survives across worlds/saves within one game instance) source of tag-suffix
 * membership data, read from {@code config/<modId>/instance_tags/instance_tags.json} for every
 * currently-attached consuming mod. Union/additive only — queried by
 * {@link dev.marie.framework.scanner.stages.CommunityTagResolutionStage} as an OR on top of real
 * vanilla tag resolution, never a replacement. No datapack tier: per-world customization already
 * works via vanilla's own tag system, which that stage already reads directly.
 *
 * <p>Scoped per {@code modId} — each consuming mod's suffix names live in their own namespace
 * and never collide with another mod's, since multiple mods may use this registry independently
 * and simultaneously.</p>
 *
 * <p>Implements marie-core's {@link InstanceTagSource} and registers itself with
 * {@code InstanceTagSourceRegistry} at mod setup — core has no compile-time dependency on
 * marie-resources, per the locked one-directional module graph.</p>
 */
@ApiStatus.Internal
public final class InstanceTagRegistry implements InstanceTagSource {

    public static final InstanceTagRegistry INSTANCE = new InstanceTagRegistry();

    private static final Gson GSON = new GsonBuilder().create();
    private static final String SUBDIR = "instance_tags";
    private static final String DATA_FILE_NAME = "instance_tags.json";

    private volatile Map<String, Map<String, Set<ResourceLocation>>> data = Map.of();

    private InstanceTagRegistry() {}

    @Override
    public boolean contains(String modId, String tagSuffix, ResourceLocation itemId) {
        Map<String, Set<ResourceLocation>> byTag = data.get(modId);
        // TEMP DEBUG - remove after diagnosis
        MarieCore.LOGGER.info("[TEMPDEBUG][InstanceTagRegistry] contains() modId={} tagSuffix={} itemId={} knownModIds={} byTagForModIdIsNull={}",
                modId, tagSuffix, itemId, data.keySet(), byTag == null);
        if (byTag == null) {
            return false;
        }
        Set<ResourceLocation> items = byTag.get(tagSuffix);
        boolean result = items != null && items.contains(itemId);
        // TEMP DEBUG - remove after diagnosis
        MarieCore.LOGGER.info("[TEMPDEBUG][InstanceTagRegistry] tagSuffix={} knownSuffixesForModId={} result={}",
                tagSuffix, byTag.keySet(), result);
        return result;
    }

    /**
     * Loads instance tag data for every mod currently registered with {@link MarieModRegistry}.
     * Safe to call repeatedly — each call fully re-enumerates attached mods and swaps in a fresh
     * immutable snapshot, so mods attached after an earlier load are picked up.
     */
    public static void load() {
        Map<String, Map<String, Set<ResourceLocation>>> next = new HashMap<>();

        for (MarieContext ctx : MarieModRegistry.getAll()) {
            String modId = ctx.modId();
            if (!MarieValidation.sanitizeModId(modId)) {
                MarieCore.LOGGER.warn("[InstanceTagRegistry] Skipping invalid modId: {}", modId);
                continue;
            }
            next.put(modId, loadForMod(modId));
        }

        INSTANCE.data = Map.copyOf(next);
    }

    public static void reload() {
        MarieCore.LOGGER.info("[InstanceTagRegistry] Reloading instance tag data");
        load();
    }

    private static Map<String, Set<ResourceLocation>> loadForMod(String modId) {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(modId).resolve(SUBDIR);
        MarieValidation.assertPathUnder(dir, FMLPaths.CONFIGDIR.get(), "InstanceTagRegistry.loadForMod");

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            MarieCore.LOGGER.error("[InstanceTagRegistry] Failed to create {}", dir, e);
            return Map.of();
        }

        try {
            writeReadmeIfAbsent(modId, dir);
        } catch (IOException e) {
            MarieCore.LOGGER.warn("[InstanceTagRegistry] Failed to write INSTANCE_TAGS_README.md for {}", modId, e);
        }

        Path file = dir.resolve(DATA_FILE_NAME);
        if (!Files.exists(file)) {
            return Map.of();
        }

        Map<String, Set<ResourceLocation>> result = parseFile(file, modId);
        if (!result.isEmpty()) {
            MarieCore.LOGGER.info("[InstanceTagRegistry] Loaded {} instance tag categor{} for {}",
                    result.size(), result.size() == 1 ? "y" : "ies", modId);
        }
        return Map.copyOf(result);
    }

    /**
     * Parses {@code instance_tags.json}. Per-key and per-entry failures are isolated: a
     * malformed category value is logged and skipped without affecting other categories, and
     * a malformed entry within an otherwise-valid category is logged and skipped without
     * discarding the rest of that category.
     */
    private static Map<String, Set<ResourceLocation>> parseFile(Path file, String modId) {
        Map<String, Set<ResourceLocation>> result = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                MarieCore.LOGGER.warn("[InstanceTagRegistry] Skipping malformed {} for {} (not a JSON object)", file, modId);
                return Map.of();
            }
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String tagSuffix = entry.getKey();
                JsonElement value = entry.getValue();
                if (value == null || !value.isJsonArray()) {
                    MarieCore.LOGGER.warn("[InstanceTagRegistry] Skipping malformed category '{}' in {} for {} (expected an array)", tagSuffix, file, modId);
                    continue;
                }
                Set<ResourceLocation> items = new LinkedHashSet<>();
                for (JsonElement el : value.getAsJsonArray()) {
                    if (el == null || !el.isJsonPrimitive()) {
                        MarieCore.LOGGER.warn("[InstanceTagRegistry] Skipping malformed entry in {} for {}:{}", file, modId, tagSuffix);
                        continue;
                    }
                    String raw = el.getAsString();
                    ResourceLocation id = ResourceLocation.tryParse(raw);
                    if (id == null) {
                        MarieCore.LOGGER.warn("[InstanceTagRegistry] Skipping invalid item id '{}' in {} for {}:{}", raw, file, modId, tagSuffix);
                        continue;
                    }
                    items.add(id);
                }
                if (!items.isEmpty()) {
                    result.put(tagSuffix, items);
                }
            }
        } catch (IOException | RuntimeException e) {
            MarieCore.LOGGER.error("[InstanceTagRegistry] Failed to parse {} for {}, skipping", file, modId, e);
            return Map.of();
        }
        return result;
    }

    /**
     * Copies a bundled {@code INSTANCE_TAGS_README.md} classpath resource into {@code instanceTagsDir}
     * if the consuming mod has one and the destination doesn't already exist. MarieLib never
     * authors README content itself — this only copies a file the consuming mod bundled, and does
     * nothing (no fallback text) when it hasn't. Mirrors
     * {@code ExcludedItemsRegistry.writeReadmeIfAbsent}'s exact mechanism.
     */
    private static void writeReadmeIfAbsent(String modId, Path instanceTagsDir) throws IOException {
        Path readme = instanceTagsDir.resolve("INSTANCE_TAGS_README.md");
        if (Files.exists(readme)) {
            return;
        }
        String resourcePath = "/data/" + modId + "/config/INSTANCE_TAGS_README.md";
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath.substring(1))) {
            if (in == null) {
                MarieCore.LOGGER.debug("[InstanceTagRegistry] No bundled INSTANCE_TAGS_README.md for {}, skipping. Tried resource path: {}", modId, resourcePath);
                return;
            }
            Files.copy(in, readme);
        }
    }
}
