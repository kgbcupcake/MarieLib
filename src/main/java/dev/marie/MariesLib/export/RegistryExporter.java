package dev.marie.MariesLib.export;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ExportResolver;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.registry.ExportResolverRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public final class RegistryExporter {

    private RegistryExporter() {}

    /**
     * Runs the named export resolver against every entry in its target registry.
     *
     * @param resolverId the resolver to run, as registered via {@code MarieAPI.registerExportResolver}
     * @return ordered map of entry id to resolved data, or empty if the resolver is not found,
     *         its registry can't be resolved, or every entry resolved to an empty map
     */
    public static Map<ResourceLocation, Map<String, Object>> run(String resolverId) {
        ExportResolverRegistry.Entry<?> entry = ExportResolverRegistry.get(resolverId);
        if (entry == null) {
            MariesLib.LOGGER.warn("[RegistryExporter] No export resolver registered: {}", resolverId);
            return Map.of();
        }

        Registry<?> rawRegistry = BuiltInRegistries.REGISTRY.get(entry.registryKey().location());
        if (rawRegistry == null) {
            MariesLib.LOGGER.warn(
                    "[RegistryExporter] Registry not found for resolver '{}': {}",
                    resolverId,
                    entry.registryKey().location()
            );
            return Map.of();
        }

        @SuppressWarnings("unchecked")
        Registry<Object> registry = (Registry<Object>) rawRegistry;

        ExportResolver<Object> resolver = uncheckedResolver(entry.resolver());
        Map<ResourceLocation, Map<String, Object>> results = new LinkedHashMap<>();
        for (Object value : registry) {
            ResourceLocation id = registry.getKey(value);
            if (id == null) {
                continue;
            }
            try {
                Map<String, Object> data = resolver.resolve(value);
                if (data != null && !data.isEmpty()) {
                    results.put(id, data);
                }
            } catch (Exception ex) {
                MariesLib.LOGGER.warn(
                        "[RegistryExporter] Resolver '{}' failed for entry {}: {}",
                        resolverId,
                        id,
                        ex.toString()
                );
                MariesLib.LOGGER.debug("[RegistryExporter] Export failure details", ex);
            }
        }

        if (results.isEmpty()) {
            return Map.of();
        }

        MariesLib.LOGGER.info("[RegistryExporter] Exported {} entries for resolver '{}'", results.size(), resolverId);
        return results;
    }

    @SuppressWarnings("unchecked")
    private static ExportResolver<Object> uncheckedResolver(ExportResolver<?> resolver) {
        return (ExportResolver<Object>) resolver;
    }
}
