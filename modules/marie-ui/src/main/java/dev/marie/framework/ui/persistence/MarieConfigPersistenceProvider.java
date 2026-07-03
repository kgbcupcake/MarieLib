package dev.marie.framework.ui.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import dev.marie.framework.core.MarieCore;
import dev.marie.framework.ui.ComponentState;
import dev.marie.framework.ui.PersistenceProvider;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The only {@link PersistenceProvider} MarieUI ships: one JSON file under config/, one entry per
 * component id, following the same Gson-backed convention marie-core uses for its own config
 * (see MariesLibConfigIO). A future server-sync or per-world provider is a drop-in replacement —
 * callers depend only on the {@link PersistenceProvider} interface.
 */
public final class MarieConfigPersistenceProvider implements PersistenceProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = MarieCore.MOD_ID + "-ui-state.json";

    private final Map<String, ComponentState> cache = new HashMap<>();
    private boolean loaded;

    @Override
    public synchronized Optional<ComponentState> load(String componentId) {
        ensureLoaded();
        return Optional.ofNullable(cache.get(componentId));
    }

    @Override
    public synchronized void save(String componentId, ComponentState state) {
        ensureLoaded();
        cache.put(componentId, state);
        writeAll();
    }

    private Path filePath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = filePath();
        if (!Files.exists(file)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) {
                return;
            }
            for (String key : root.keySet()) {
                JsonObject o = root.getAsJsonObject(key);
                cache.put(key, new ComponentState(
                        o.get("x").getAsInt(),
                        o.get("y").getAsInt(),
                        o.get("width").getAsInt(),
                        o.get("height").getAsInt(),
                        o.has("collapsed") && o.get("collapsed").getAsBoolean()
                ));
            }
        } catch (IOException e) {
            MarieCore.LOGGER.error("[MarieUI] Failed to load {}, discarding cached component state", file, e);
        }
    }

    private void writeAll() {
        Path file = filePath();
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            for (Map.Entry<String, ComponentState> entry : cache.entrySet()) {
                ComponentState s = entry.getValue();
                JsonObject o = new JsonObject();
                o.addProperty("x", s.x());
                o.addProperty("y", s.y());
                o.addProperty("width", s.width());
                o.addProperty("height", s.height());
                o.addProperty("collapsed", s.collapsed());
                root.add(entry.getKey(), o);
            }
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            MarieCore.LOGGER.error("[MarieUI] Failed to save {}", file, e);
        }
    }
}
