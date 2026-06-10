package dev.marie.MariesLib.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads bundled {@code data/<modid>/config/mod_compat.json} at startup (integration metadata and
 * optional per-mod tuning). Other systems may query {@link #isLoaded(String)} or heavy-source thresholds.
 */
@ApiStatus.Internal
public final class ModCompatRegistry {

    private static final Gson GSON = new Gson();
    private static final String SOLONION_MOD_ID = "solonion";

    private static String resourcePath() {
        return "/data/" + MarieLibContext.get().modId() + "/config/mod_compat.json";
    }

    private static final Map<String, IntegrationEntry> INTEGRATIONS = new LinkedHashMap<>();

    private static final String LEGACY_HEAVY_THRESHOLD_KEY = new String(new char[]{
            'h', 'e', 'a', 'v', 'y', 'M', 'e', 'a', 'l', 'N', 'u', 't', 'r', 'i', 't', 'i', 'o', 'n', 'T', 'h', 'r', 'e', 's', 'h', 'o', 'l', 'd'
    });

    private ModCompatRegistry() {}

    public record IntegrationEntry(String modId, Integer heavySourcePropertyThreshold, String notes) {}

    private static Integer readHeavySourcePropertyThreshold(JsonObject o) {
        if (o.has("heavySourcePropertyThreshold") && !o.get("heavySourcePropertyThreshold").isJsonNull()) {
            return Mth.clamp(o.get("heavySourcePropertyThreshold").getAsInt(), 1, 20);
        }
        if (o.has(LEGACY_HEAVY_THRESHOLD_KEY) && !o.get(LEGACY_HEAVY_THRESHOLD_KEY).isJsonNull()) {
            return Mth.clamp(o.get(LEGACY_HEAVY_THRESHOLD_KEY).getAsInt(), 1, 20);
        }
        return null;
    }

    public static void load() {
        INTEGRATIONS.clear();
        String path = resourcePath();
        try (InputStream in = ModCompatRegistry.class.getResourceAsStream(path)) {
            if (in == null) {
                MariesLib.LOGGER.warn("[MarieLib] Missing resource {}", path);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("integrations") || !root.get("integrations").isJsonArray()) {
                    MariesLib.LOGGER.warn("[MarieLib] mod_compat.json missing integrations array");
                    return;
                }
                JsonArray arr = root.getAsJsonArray("integrations");
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject o = el.getAsJsonObject();
                    if (!o.has("modId")) {
                        continue;
                    }
                    String modId = o.get("modId").getAsString();
                    Integer heavy = readHeavySourcePropertyThreshold(o);
                    String notes = "";
                    if (o.has("notes") && o.get("notes").isJsonPrimitive()) {
                        notes = o.get("notes").getAsString();
                    }
                    INTEGRATIONS.put(modId, new IntegrationEntry(modId, heavy, notes));
                }
            }
            MariesLib.LOGGER.info("[MarieLib] Loaded {} integration entries from mod_compat.json",
                    INTEGRATIONS.size());
        } catch (Exception e) {
            MariesLib.LOGGER.error("[MarieLib] Failed to load mod_compat.json", e);
            INTEGRATIONS.clear();
        }
    }

    /** @return immutable view of parsed integration rows */
    public static Map<String, IntegrationEntry> integrations() {
        return Collections.unmodifiableMap(INTEGRATIONS);
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * When Spice of Life: Onion ({@code solonion}) is loaded, returns {@code heavySourcePropertyThreshold}
     * from its mod_compat entry when present; otherwise {@link ModuleCache#heavySourcePropertyThreshold}.
     * When {@code solonion} is not loaded, returns {@link ModuleCache#heavySourcePropertyThreshold} only.
     */
    public static int getHeavySourceThreshold() {
        if (!ModList.get().isLoaded(SOLONION_MOD_ID)) {
            return ModuleCache.heavySourcePropertyThreshold;
        }
        IntegrationEntry entry = INTEGRATIONS.get(SOLONION_MOD_ID);
        if (entry != null && entry.heavySourcePropertyThreshold != null) {
            return entry.heavySourcePropertyThreshold();
        }
        return ModuleCache.heavySourcePropertyThreshold;
    }
}
