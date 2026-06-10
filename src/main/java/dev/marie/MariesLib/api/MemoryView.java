package dev.marie.MariesLib.api;

import dev.marie.MariesLib.api.ApiStatus;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Read-only view into a player's source memory, exposing recent consumption
 * history without allowing mutation.
 *
 * <p>Use this to query what a player has applied recently, check for tracking
 * variety, or implement diminishing-returns mechanics.</p>
 */
@ApiStatus.Stable
public interface MemoryView {

    /**
     * Returns an ordered list of recently consumed source identifiers,
     * from most recent to oldest.
     *
     * @return an unmodifiable list of source {@link ResourceLocation} identifiers
     */
    List<ResourceLocation> getRecentSources();

    /**
     * Checks whether the player has consumed the specified source within
     * the memory window (determined by configuration).
     *
     * @param sourceId the registry identifier of the source item to check
     * @return {@code true} if the source exists in the player's recent memory
     */
    boolean hasSourceRecently(ResourceLocation sourceId);

    /**
     * Returns the time in game ticks since the player last applied the specified source.
     *
     * @param sourceId the registry identifier of the source item to query
     * @return the elapsed ticks since last application, or {@code -1} if never applied
     *         within the memory window
     */
    long getTimeSinceSource(ResourceLocation sourceId);
}
