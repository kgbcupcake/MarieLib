package dev.marie.framework.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.reporting.ExportResolver;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.Map;

@ApiStatus.Internal
public final class ExportResolverRegistry {

    public record Entry<T>(String resolverId, ResourceKey<? extends Registry<T>> registryKey, ExportResolver<T> resolver) {}

    private static final class Core extends AbstractRegistry<String, Entry<?>> {
        Core() {
            super("ExportResolverRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private ExportResolverRegistry() {}

    public static <T> void register(String resolverId, ResourceKey<? extends Registry<T>> registryKey, ExportResolver<T> resolver) {
        if (INSTANCE.contains(resolverId)) {
            throw new IllegalArgumentException("Export resolver already registered: " + resolverId);
        }
        INSTANCE.register(resolverId, new Entry<>(resolverId, registryKey, resolver));
    }

    public static Entry<?> get(String resolverId) {
        return INSTANCE.get(resolverId);
    }

    public static Map<String, Entry<?>> getAll() {
        return INSTANCE.entries();
    }
}
