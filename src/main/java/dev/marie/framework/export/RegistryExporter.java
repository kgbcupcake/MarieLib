package dev.marie.framework.export;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Supplier;

@ApiStatus.Stable
public final class RegistryExporter {

    private RegistryExporter() {}

    public static Map<ResourceLocation, Map<String, Object>> run(String resolverId) {
        Supplier<Map<ResourceLocation, Map<String, Object>>> supplier =
                ExportResolverRegistry.getExportSupplier(resolverId);
        if (supplier == null) return Map.of();
        Map<ResourceLocation, Map<String, Object>> result = supplier.get();
        return result != null ? result : Map.of();
    }
}
