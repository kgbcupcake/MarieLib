package dev.marie.framework.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.IMarieLibConfig;
import dev.marie.framework.core.MariesLib;
import dev.marie.framework.util.MarieValidation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

@ApiStatus.Internal
public final class ExportWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ExportWriter() {}

    /**
     * Runs the named export resolver and writes its output to
     * {@code config/<modid>/<resolverId>_export.json}.
     *
     * @param resolverId the resolver to run and write
     * @return the path written to, or null if the resolver produced no data
     */
    public static Path writeExport(String resolverId) {
        Map<ResourceLocation, Map<String, Object>> results = RegistryExporter.run(resolverId);
        if (results.isEmpty()) {
            MariesLib.LOGGER.warn("[ExportWriter] No data to write for resolver '{}'", resolverId);
            return null;
        }

        Path configDir = FMLPaths.CONFIGDIR.get().resolve(IMarieLibConfig.get().modId());
        Path file = configDir.resolve(resolverId + "_export.json");

        Map<String, Map<String, Object>> sorted = new TreeMap<>();
        for (Map.Entry<ResourceLocation, Map<String, Object>> e : results.entrySet()) {
            sorted.put(e.getKey().toString(), e.getValue());
        }

        JsonArray arr = new JsonArray();
        for (Map.Entry<String, Map<String, Object>> e : sorted.entrySet()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", e.getKey());
            JsonObject dataObj = new JsonObject();
            for (Map.Entry<String, Object> field : e.getValue().entrySet()) {
                addField(dataObj, field.getKey(), field.getValue());
            }
            obj.add("data", dataObj);
            arr.add(obj);
        }

        try {
            Files.createDirectories(configDir);
            MarieValidation.assertPathUnder(file, configDir, "ExportWriter");
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(arr, w);
            }
            MariesLib.LOGGER.info("[ExportWriter] Wrote {} entries to {}", results.size(), file);
            return file;
        } catch (IOException e) {
            MariesLib.LOGGER.error("[ExportWriter] Failed to write export for resolver '{}'", resolverId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void addField(JsonObject obj, String key, Object value) {
        if (value instanceof Number n) {
            obj.addProperty(key, n);
        } else if (value instanceof Boolean b) {
            obj.addProperty(key, b);
        } else if (value instanceof Map<?, ?> nested) {
            JsonObject nestedObj = new JsonObject();
            for (Map.Entry<?, ?> e : nested.entrySet()) {
                addField(nestedObj, String.valueOf(e.getKey()), e.getValue());
            }
            obj.add(key, nestedObj);
        } else if (value == null) {
            obj.add(key, JsonNull.INSTANCE);
        } else {
            obj.addProperty(key, String.valueOf(value));
        }
    }
}
