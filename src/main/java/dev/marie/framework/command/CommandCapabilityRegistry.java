package dev.marie.MariesLib.command;

import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class CommandCapabilityRegistry {

    private static final Map<ResourceLocation, Map<ResourceLocation, CommandCapability>> REGISTRY
            = new ConcurrentHashMap<>();

    private CommandCapabilityRegistry() {}

    public static void register(ResourceLocation modId, ResourceLocation capability,
                                CommandCapability handler) {
        REGISTRY.computeIfAbsent(modId, k -> new ConcurrentHashMap<>())
                .put(capability, handler);
    }

    public static Optional<CommandCapability> get(ResourceLocation modId,
                                                   ResourceLocation capability) {
        Map<ResourceLocation, CommandCapability> mod = REGISTRY.get(modId);
        if (mod == null) return Optional.empty();
        return Optional.ofNullable(mod.get(capability));
    }

    public static Set<ResourceLocation> capabilitiesFor(ResourceLocation modId) {
        Map<ResourceLocation, CommandCapability> mod = REGISTRY.get(modId);
        return mod != null ? Collections.unmodifiableSet(mod.keySet()) : Set.of();
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
