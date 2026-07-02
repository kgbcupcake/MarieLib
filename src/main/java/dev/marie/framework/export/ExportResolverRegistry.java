package dev.marie.framework.export;

import com.mojang.logging.LogUtils;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ExportResolver;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry of named export resolvers for the {@code /marie dump <resolverId>} command.
 */
@ApiStatus.Internal
public final class ExportResolverRegistry {

    private static final Map<String, Supplier<Path>> RESOLVERS = new LinkedHashMap<>();
    private static final Map<String, Supplier<Map<ResourceLocation, Map<String, Object>>>> EXPORT_SUPPLIERS =
            new LinkedHashMap<>();

    private ExportResolverRegistry() {}

    public static void register(String key, Supplier<Path> resolver) {
        RESOLVERS.put(key, resolver);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerWithRegistry(
            String key,
            ResourceKey<? extends Registry<T>> registryKey,
            ExportResolver<T> resolver) {
        EXPORT_SUPPLIERS.put(key, () -> {
            Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
            if (registry == null) return Map.of();
            Map<ResourceLocation, Map<String, Object>> result = new LinkedHashMap<>();
            for (T item : registry) {
                ResourceLocation id = registry.getKey(item);
                if (id != null) {
                    Map<String, Object> resolved = resolver.resolve(item);
                    if (resolved != null && !resolved.isEmpty()) {
                        result.put(id, resolved);
                    }
                }
            }
            return result;
        });
        RESOLVERS.put(key, () -> {
            Supplier<Map<ResourceLocation, Map<String, Object>>> exporter = EXPORT_SUPPLIERS.get(key);
            if (exporter == null) return null;
            Map<ResourceLocation, Map<String, Object>> data = exporter.get();
            if (data == null || data.isEmpty()) return null;
            try {
                String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                        .withZone(ZoneOffset.UTC).format(Instant.now());
                Path path = FMLPaths.CONFIGDIR.get()
                        .resolve("marie").resolve("export")
                        .resolve(key + "_" + timestamp + ".json");
                Files.createDirectories(path.getParent());
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                Files.writeString(path, gson.toJson(data));
                return path;
            } catch (IOException e) {
                LogUtils.getLogger().warn("[ExportResolverRegistry] File write failed for '{}': {}", key, e.getMessage());
                return null;
            }
        });
    }

    public static Map<String, Supplier<Path>> getAll() {
        return Collections.unmodifiableMap(RESOLVERS);
    }

    public static @Nullable Supplier<Path> get(String key) {
        return RESOLVERS.get(key);
    }

    public static @Nullable Supplier<Map<ResourceLocation, Map<String, Object>>> getExportSupplier(String key) {
        return EXPORT_SUPPLIERS.get(key);
    }
}
