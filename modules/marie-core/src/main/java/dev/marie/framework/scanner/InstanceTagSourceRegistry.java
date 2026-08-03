package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApiStatus.Internal
public final class InstanceTagSourceRegistry {

    private static final List<InstanceTagSource> SOURCES = new CopyOnWriteArrayList<>();

    private InstanceTagSourceRegistry() {}

    public static void register(InstanceTagSource source) {
        SOURCES.add(source);
    }

    public static void clear() {
        SOURCES.clear();
    }

    /**
     * Returns {@code true} if any registered source reports {@code itemId} as a member of
     * {@code tagSuffix} for {@code modId}. OR'd across all registered sources — there will
     * typically be exactly one, but this doesn't assume that.
     */
    public static boolean contains(String modId, String tagSuffix, ResourceLocation itemId) {
        for (InstanceTagSource source : SOURCES) {
            if (source.contains(modId, tagSuffix, itemId)) {
                return true;
            }
        }
        return false;
    }
}
