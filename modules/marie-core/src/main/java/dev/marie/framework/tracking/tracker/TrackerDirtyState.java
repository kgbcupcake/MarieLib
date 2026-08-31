package dev.marie.framework.tracking.tracker;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player bookkeeping for the high-frequency tracker live-value sync sweep:
 * which trackers changed since the last sync, and when that player was last synced.
 * Touched by {@link MarieTracking#incrementTracker} (marks dirty) and
 * {@link TrackerManager#sweepDirtySync} (drains and throttles). Internal — never exposed to
 * consuming mods.
 */
@ApiStatus.Internal
final class TrackerDirtyState {

    private static final Map<UUID, Set<ResourceLocation>> DIRTY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SYNC_TICK = new ConcurrentHashMap<>();

    private TrackerDirtyState() {}

    static void markDirty(UUID playerId, ResourceLocation trackerId) {
        DIRTY.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(trackerId);
    }

    static boolean hasDirty(UUID playerId) {
        Set<ResourceLocation> set = DIRTY.get(playerId);
        return set != null && !set.isEmpty();
    }

    /** Atomically removes and returns the dirty set for a player, or an empty set if none. */
    static Set<ResourceLocation> drainDirty(UUID playerId) {
        Set<ResourceLocation> set = DIRTY.remove(playerId);
        return set != null ? set : Set.of();
    }

    static long lastSyncTick(UUID playerId) {
        return LAST_SYNC_TICK.getOrDefault(playerId, 0L);
    }

    static void setLastSyncTick(UUID playerId, long tick) {
        LAST_SYNC_TICK.put(playerId, tick);
    }

    /** Clears all bookkeeping for a player, e.g. on logout. */
    static void clearPlayer(UUID playerId) {
        DIRTY.remove(playerId);
        LAST_SYNC_TICK.remove(playerId);
    }
}
