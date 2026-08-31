package dev.marie.framework.tracking.tracker;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.marie.framework.tracking.tracker.definition.TrackingPeriodState;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of tracker state, keyed by tracker id — the client-side counterpart to the
 * server's {@code TrackingData} tracker maps. Populated by {@code TrackerNetworking}'s payload
 * handlers (live-value sync and history/period-state sync); read by
 * {@link MarieTracking#getCurrentTrackerValue} / {@link MarieTracking#getTrackerHistory} when
 * called with a client-side (non-{@code ServerPlayer}) player instance.
 *
 * <p>Mirrors the shape of {@code ColorPreviewOverrides} — a simple transient, non-persisted
 * keyed cache. Backed by {@link ConcurrentHashMap} since network and render/client threads both
 * touch it.</p>
 */
@ApiStatus.Internal
public final class ClientTrackerCache {

    private static final Map<ResourceLocation, Float> CURRENT_VALUES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<TrackerHistoryEntry>> HISTORY = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, TrackingPeriodState> PERIOD_STATES = new ConcurrentHashMap<>();

    private ClientTrackerCache() {}

    public static void setCurrentValue(ResourceLocation trackerId, float value) {
        CURRENT_VALUES.put(trackerId, value);
    }

    static float getCurrentValue(ResourceLocation trackerId) {
        return CURRENT_VALUES.getOrDefault(trackerId, 0f);
    }

    public static void setHistory(ResourceLocation trackerId, List<TrackerHistoryEntry> history) {
        HISTORY.put(trackerId, history);
    }

    static List<TrackerHistoryEntry> getHistory(ResourceLocation trackerId) {
        return HISTORY.getOrDefault(trackerId, List.of());
    }

    public static void setPeriodState(ResourceLocation trackerId, TrackingPeriodState state) {
        PERIOD_STATES.put(trackerId, state);
    }

    public static TrackingPeriodState getPeriodState(ResourceLocation trackerId) {
        return PERIOD_STATES.get(trackerId);
    }

    /** Clears all cached tracker state, e.g. on disconnect. */
    public static void clear() {
        CURRENT_VALUES.clear();
        HISTORY.clear();
        PERIOD_STATES.clear();
    }
}
