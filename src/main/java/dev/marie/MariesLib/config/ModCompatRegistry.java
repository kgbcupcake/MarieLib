package dev.marie.MariesLib.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.marie.MariesLib.core.MariesLib;
import net.neoforged.fml.ModList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads bundled {@code data/<modid>/config/mod_compat.json} at startup (integration metadata).
 * Other systems may query {@link #isLoaded(String)}.
 */
@ApiStatus.Internal
public final class ModCompatRegistry {

    private static final Gson GSON = new Gson();

    private static String resourcePath() {
        return "/data/" + IMarieLibConfig.get().modId() + "/config/mod_compat.json";
    }

    private static final Map<String, IntegrationEntry> INTEGRATIONS = new LinkedHashMap<>();

    private ModCompatRegistry() {}

    public record IntegrationEntry(String modId, String notes) {}

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
                    String notes = "";
                    if (o.has("notes") && o.get("notes").isJsonPrimitive()) {
                        notes = o.get("notes").getAsString();
                    }
                    INTEGRATIONS.put(modId, new IntegrationEntry(modId, notes));
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
}
