package dev.marie.framework.tracking.tracker;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marieapi.MarieAPIState;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.definition.TrackerDefinition;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.marie.framework.tracking.tracker.network.TrackerNetworking;
import dev.marie.framework.tracking.tracker.registry.TrackerRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Set;

/**
 * Public facade for the MarieLib generic tracker/period-history subsystem: registering trackers
 * and reading their accumulated/historical state. Self-contained and separate from
 * {@link dev.marie.framework.api.marieapi.MarieAPI} — trackers have their own client-sync
 * concerns (see {@link #getCurrentTrackerValue}) rather than routing through the general API
 * surface.
 */
@ApiStatus.Stable
public final class MarieTracking {

    private MarieTracking() {}

    /**
     * Registers a tracker definition with the generic tracker/period-history framework.
     * MarieLib manages accumulation, period boundaries, and bounded history — it has no
     * domain knowledge of what the tracker measures.
     *
     * <p>Must be called during mod initialization or datapack reload. Re-registering an
     * already-registered id is safe and replaces the existing definition rather than throwing —
     * every {@code /reload} wipes {@link TrackerRegistry}, so consumers that want their trackers
     * to survive a reload should call this again from {@code MarieContext.reloadBroadcastHook()}
     * (see {@link dev.marie.framework.handler.ReloadGuardListener#reloadAndBroadcast}); nothing
     * else re-invokes registration code automatically.</p>
     *
     * @param definition the tracker definition to register
     * @throws IllegalStateException    if called outside the registration window
     * @throws IllegalArgumentException if {@code definition} is null, retention is less than 1,
     *                                   or retention exceeds the configured maximum
     */
    @ApiStatus.Stable
    public static void registerTracker(TrackerDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerTracker");
        if (definition == null) {
            throw new IllegalArgumentException("registerTracker: definition must not be null");
        }
        TrackerRegistry.register(definition);
    }

    /**
     * Adds {@code amount} to a player's current-period accumulator for the given tracker.
     *
     * <p>For a tracker registered with {@link TrackerDefinition.Builder#immediateSync}, the
     * updated value is sent to the player's client immediately, bypassing the throttled sweep
     * entirely for this tracker. Otherwise, the tracker is marked dirty for that player so the
     * next throttled sync sweep ({@code IMarieConfig#trackerSyncIntervalTicks()}) pushes the new
     * value to their client.</p>
     *
     * <p>No-ops if the tracker system is disabled via configuration.</p>
     *
     * @param player    the server-side player to update
     * @param trackerId the registered tracker's id
     * @param amount    the amount to add to the current period's accumulator
     * @throws IllegalArgumentException if no tracker is registered with {@code trackerId}
     */
    @ApiStatus.Stable
    public static void incrementTracker(ServerPlayer player, ResourceLocation trackerId, float amount) {
        if (!IMarieConfig.get().trackerSystemEnabled()) {
            return;
        }
        TrackerDefinition definition = TrackerRegistry.get(trackerId);
        if (definition == null) {
            throw new IllegalArgumentException("incrementTracker: no tracker registered with id " + trackerId);
        }
        TrackingData tracking = TrackingAttachment.getData(player);
        float current = tracking.trackingAccumulators.getOrDefault(trackerId, 0f);
        tracking.trackingAccumulators.put(trackerId, current + amount);
        TrackingAttachment.setData(player, tracking);
        if (definition.isImmediateSync()) {
            TrackerNetworking.sendLiveValues(player, Set.of(trackerId), tracking);
        } else {
            TrackerDirtyState.markDirty(player.getUUID(), trackerId);
        }
    }

    /**
     * Returns the current-period accumulated value for a tracker.
     *
     * <p>Branches on side: for a server-side {@link ServerPlayer}, reads the live server
     * attachment directly. For a client-side player instance (e.g. {@code LocalPlayer}), reads
     * {@link ClientTrackerCache}, populated by the throttled live-value sync sweep — attachments
     * are server-authoritative and are not themselves kept live-synced to the client, so calling
     * this client-side against the attachment would silently read stale/default data. This is
     * the fix for that footgun, not just a relocation of it.</p>
     *
     * @param player    the player to query
     * @param trackerId the registered tracker's id
     * @return the current accumulator value, or {@code 0f} if unset
     */
    @ApiStatus.Stable
    public static float getCurrentTrackerValue(Player player, ResourceLocation trackerId) {
        if (player instanceof ServerPlayer serverPlayer) {
            TrackingData tracking = TrackingAttachment.getData(serverPlayer);
            return tracking.trackingAccumulators.getOrDefault(trackerId, 0f);
        }
        return ClientTrackerCache.getCurrentValue(trackerId);
    }

    /**
     * Returns a player's completed-period history for a tracker, most recent first, bounded by
     * the tracker's configured retention.
     *
     * <p>Branches on side the same way as {@link #getCurrentTrackerValue}: server-side reads the
     * attachment directly, client-side reads {@link ClientTrackerCache}, populated by the
     * on-login full snapshot and per-period targeted resyncs.</p>
     *
     * @param player    the player to query
     * @param trackerId the registered tracker's id
     * @return an unmodifiable list of history entries, most recent first
     */
    @ApiStatus.Stable
    public static List<TrackerHistoryEntry> getTrackerHistory(Player player, ResourceLocation trackerId) {
        if (player instanceof ServerPlayer serverPlayer) {
            TrackingData tracking = TrackingAttachment.getData(serverPlayer);
            List<TrackerHistoryEntry> history = tracking.trackingHistory.get(trackerId);
            if (history == null || history.isEmpty()) {
                return List.of();
            }
            TrackerDefinition definition = TrackerRegistry.get(trackerId);
            int retention = definition != null ? definition.getRetention() : history.size();
            int bound = Mth.clamp(retention, 0, history.size());
            return List.copyOf(history.subList(0, bound));
        }
        return ClientTrackerCache.getHistory(trackerId);
    }
}
