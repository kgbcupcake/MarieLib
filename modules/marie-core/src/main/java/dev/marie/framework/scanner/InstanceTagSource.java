package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

/**
 * Contract for an instance-level (survives across worlds/saves within one game instance) source
 * of community-tag membership data. Queried by {@link dev.marie.framework.scanner.stages.CommunityTagResolutionStage}
 * as an additive union on top of whatever the real vanilla {@code TagKey<Item>} already resolves
 * to — never an override/replace of vanilla tag resolution.
 *
 * <p>Implemented by marie-resources's {@code InstanceTagRegistry}, which registers itself with
 * {@link InstanceTagSourceRegistry} — core has no compile-time dependency on marie-resources, per
 * the locked one-directional module graph.</p>
 */
@ApiStatus.Internal
@FunctionalInterface
public interface InstanceTagSource {
    boolean contains(String modId, String tagSuffix, ResourceLocation itemId);
}
