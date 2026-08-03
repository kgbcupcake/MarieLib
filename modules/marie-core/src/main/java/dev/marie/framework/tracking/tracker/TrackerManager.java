package dev.marie.framework.tracking.tracker;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.definition.TrackerDefinition;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.marie.framework.tracking.tracker.definition.TrackerPeriod;
import dev.marie.framework.tracking.tracker.definition.TrackingPeriodState;
import dev.marie.framework.tracking.tracker.network.TrackerNetworking;
import dev.marie.framework.tracking.tracker.registry.TrackerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

/**
 * Drives tracker period boundaries and history for the generic tracker/period-history framework.
 * MarieLib has no domain knowledge of what a tracker measures — it only manages accumulation,
 * boundaries, and bounded history.
 */
@ApiStatus.Internal
public final class TrackerManager {

    private TrackerManager() {}

    /**
     * Checks all DAILY/WEEKLY/MONTHLY/REAL_TIME tracker boundaries for {@code player}. Called once
     * per tick from the per-player tick loop. SESSION trackers are opened/closed from the player-join
     * listener; CUSTOM trackers are closed via {@link #fireCustomTrackerReset}.
     */
    @ApiStatus.Internal
    public static void checkTrackers(ServerPlayer player, TrackingData tracking) {
        if (!IMarieConfig.get().trackerSystemEnabled()) {
            return;
        }
        long nowGameTimeMs = player.level().getGameTime();
        for (TrackerDefinition definition : TrackerRegistry.getAll()) {
            TrackerPeriod period = definition.getPeriod();
            if (period == TrackerPeriod.SESSION || period == TrackerPeriod.CUSTOM) {
                continue;
            }
            processDefinition(player, tracking, definition, nowGameTimeMs);
        }
    }

    /**
     * Opens the SESSION-period trackers for a fresh login, first closing any period left open from
     * a previous session (e.g. an unclean disconnect). Called exactly once per login from the
     * player-join listener.
     */
    @ApiStatus.Internal
    public static void openSessionTrackers(ServerPlayer player, TrackingData tracking) {
        if (!IMarieConfig.get().trackerSystemEnabled()) {
            return;
        }
        long nowGameTimeMs = player.level().getGameTime();
        for (TrackerDefinition definition : TrackerRegistry.getAll()) {
            if (definition.getPeriod() != TrackerPeriod.SESSION) {
                continue;
            }
            ResourceLocation id = definition.getId();
            TrackingPeriodState state = tracking.trackerPeriodStates.get(id);
            if (state == null) {
                tracking.trackerPeriodStates.put(id, openPeriod(definition, nowGameTimeMs));
            } else {
                closePeriodAndOpenNext(player, tracking, definition, id, state, nowGameTimeMs);
            }
        }
    }

    /**
     * Closes and reopens a CUSTOM-period tracker on demand. The consuming mod decides when a
     * CUSTOM period ends and calls this directly.
     */
    @ApiStatus.Internal
    public static void fireCustomTrackerReset(ServerPlayer player, TrackingData tracking, ResourceLocation trackerId) {
        if (!IMarieConfig.get().trackerSystemEnabled()) {
            return;
        }
        TrackerDefinition definition = TrackerRegistry.get(trackerId);
        if (definition == null || definition.getPeriod() != TrackerPeriod.CUSTOM) {
            return;
        }
        long nowGameTimeMs = player.level().getGameTime();
        TrackingPeriodState state = tracking.trackerPeriodStates.get(trackerId);
        if (state == null) {
            tracking.trackerPeriodStates.put(trackerId, openPeriod(definition, nowGameTimeMs));
            return;
        }
        closePeriodAndOpenNext(player, tracking, definition, trackerId, state, nowGameTimeMs);
    }

    /** Clears dirty-sync bookkeeping for a player, e.g. on logout. */
    @ApiStatus.Internal
    public static void clearDirtySyncState(UUID playerId) {
        TrackerDirtyState.clearPlayer(playerId);
    }

    /**
     * Throttled dirty-value push: if {@code player} has trackers marked dirty by
     * {@link MarieTracking#incrementTracker} and at least {@code IMarieConfig#trackerSyncIntervalTicks()}
     * have passed since their last sync, sends just those trackers' current values and clears
     * the dirty set. Called once per player tick, piggybacked on the same loop as
     * {@link #checkTrackers}.
     */
    @ApiStatus.Internal
    public static void sweepDirtySync(ServerPlayer player) {
        if (!IMarieConfig.get().trackerSystemEnabled()) {
            return;
        }
        UUID playerId = player.getUUID();
        if (!TrackerDirtyState.hasDirty(playerId)) {
            return;
        }
        long nowGameTime = player.level().getGameTime();
        int interval = Math.max(1, IMarieConfig.get().trackerSyncIntervalTicks());
        if (nowGameTime - TrackerDirtyState.lastSyncTick(playerId) < interval) {
            return;
        }
        Set<ResourceLocation> dirty = TrackerDirtyState.drainDirty(playerId);
        if (dirty.isEmpty()) {
            return;
        }
        TrackingData tracking = TrackingAttachment.getData(player);
        TrackerNetworking.sendLiveValues(player, dirty, tracking);
        TrackerDirtyState.setLastSyncTick(playerId, nowGameTime);
    }

    private static void processDefinition(ServerPlayer player, TrackingData tracking,
            TrackerDefinition definition, long nowGameTimeMs) {
        ResourceLocation id = definition.getId();
        TrackingPeriodState state = tracking.trackerPeriodStates.get(id);
        if (state == null) {
            // First-period initialization: open only, never backfill/synthesize a history entry.
            tracking.trackerPeriodStates.put(id, openPeriod(definition, nowGameTimeMs));
            return;
        }
        if (isBoundaryDue(definition, state, nowGameTimeMs)) {
            closePeriodAndOpenNext(player, tracking, definition, id, state, nowGameTimeMs);
        }
    }

    /** The seam for later optimization (e.g. batching boundary checks instead of per-tracker per-tick). */
    private static boolean isBoundaryDue(TrackerDefinition definition, TrackingPeriodState state, long nowGameTimeMs) {
        return switch (definition.getPeriod()) {
            case DAILY, WEEKLY, MONTHLY -> nowGameTimeMs >= state.periodEnd();
            case REAL_TIME -> System.currentTimeMillis() >= state.periodEnd();
            case SESSION, CUSTOM -> false;
        };
    }

    private static void closePeriodAndOpenNext(ServerPlayer player, TrackingData tracking,
            TrackerDefinition definition, ResourceLocation id, TrackingPeriodState state, long nowGameTimeMs) {
        float value = tracking.trackingAccumulators.getOrDefault(id, 0f);
        long periodEnd = switch (definition.getPeriod()) {
            case SESSION, CUSTOM -> nowGameTimeMs;
            case DAILY, WEEKLY, MONTHLY, REAL_TIME -> state.periodEnd();
        };
        TrackerHistoryEntry entry = new TrackerHistoryEntry(
                id, definition.getPeriod().configId(), state.periodStart(), periodEnd, value);
        tracking.appendTrackerHistory(id, entry, definition.getRetention());
        tracking.trackingAccumulators.put(id, 0f);
        tracking.trackerPeriodStates.put(id, openPeriod(definition, nowGameTimeMs));
        TrackerNetworking.sendPeriodResync(player, id, tracking);
        if (MarieContext.isRegistered()) {
            MarieContext.get().onTrackerPeriodCompletedHook().accept(player, entry);
        }
    }

    private static TrackingPeriodState openPeriod(TrackerDefinition definition, long nowGameTimeMs) {
        return switch (definition.getPeriod()) {
            case DAILY -> {
                long dayNumber = nowGameTimeMs / 24000L;
                yield new TrackingPeriodState(nowGameTimeMs, (dayNumber + 1) * 24000L);
            }
            case WEEKLY -> {
                long windowTicks = IMarieConfig.get().trackerWeeklyPeriodDays() * 24000L;
                yield new TrackingPeriodState(nowGameTimeMs, nowGameTimeMs + windowTicks);
            }
            case MONTHLY -> {
                long windowTicks = (long) IMarieConfig.get().trackerMonthlyPeriodDays() * 24000L;
                yield new TrackingPeriodState(nowGameTimeMs, nowGameTimeMs + windowTicks);
            }
            case REAL_TIME -> {
                long nowMs = System.currentTimeMillis();
                yield new TrackingPeriodState(nowMs, nowMs + definition.getRealTimeDurationMs());
            }
            case SESSION, CUSTOM -> new TrackingPeriodState(nowGameTimeMs, nowGameTimeMs);
        };
    }
}
