package dev.marie.framework.api.marieapi;

import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.definition.TrackerDefinition;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.marie.framework.tracking.tracker.registry.TrackerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

import java.util.List;

final class TrackerRegistrationDelegate {

    private TrackerRegistrationDelegate() {}

    static void registerTracker(TrackerDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerTracker");
        if (definition == null) {
            throw new IllegalArgumentException("registerTracker: definition must not be null");
        }
        TrackerRegistry.register(definition);
    }

    static void incrementTracker(ServerPlayer player, ResourceLocation trackerId, float amount) {
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
    }

    static float getCurrentTrackerValue(Player player, ResourceLocation trackerId) {
        TrackingData tracking = TrackingAttachment.getData(player);
        return tracking.trackingAccumulators.getOrDefault(trackerId, 0f);
    }

    static List<TrackerHistoryEntry> getTrackerHistory(Player player, ResourceLocation trackerId) {
        TrackingData tracking = TrackingAttachment.getData(player);
        List<TrackerHistoryEntry> history = tracking.trackingHistory.get(trackerId);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        TrackerDefinition definition = TrackerRegistry.get(trackerId);
        int retention = definition != null ? definition.getRetention() : history.size();
        int bound = Mth.clamp(retention, 0, history.size());
        return List.copyOf(history.subList(0, bound));
    }
}
