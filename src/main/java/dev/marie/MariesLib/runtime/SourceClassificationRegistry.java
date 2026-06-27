package dev.marie.MariesLib.runtime;

import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public read-only view of external source classifications registered via {@link SourceRegistry}.
 */
@ApiStatus.Stable
public final class SourceClassificationRegistry {

    private SourceClassificationRegistry() {}

    public record SourceClassification(String sourceId, Map<String, Float> values) {}

    public static Map<ResourceLocation, SourceClassification> getAll() {
        Map<ResourceLocation, Map<String, Float>> raw = SourceRegistry.getAllExternalView();
        Map<ResourceLocation, SourceClassification> result = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Map<String, Float>> entry : raw.entrySet()) {
            result.put(entry.getKey(),
                    new SourceClassification(entry.getKey().toString(),
                            Collections.unmodifiableMap(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }
}
