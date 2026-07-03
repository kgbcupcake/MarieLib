package dev.marie.framework.tracking;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ApplicationHistoryView;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.core.MarieCore;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete implementation of {@link ApplicationHistoryView} backed by a player's {@link TrackingData}.
 */
@ApiStatus.Internal
public final class TrackingDataApplicationHistoryView implements ApplicationHistoryView {

    private final TrackingData trackingData;

    public TrackingDataApplicationHistoryView(TrackingData trackingData) {
        this.trackingData = trackingData;
    }

    @Override
    public List<ResourceLocation> getRecentSources() {
        long halfLifeMs = IMarieConfig.get().memoryWindowMinutes() * 60_000L;
        long gameTimeMs = trackingData.lastTickTime > 0 ? trackingData.lastTickTime : System.currentTimeMillis();

        return trackingData.sourceMemory.entrySet().stream()
                .filter(e -> !e.getValue().isEffectivelyExpired(halfLifeMs, gameTimeMs, 0.1f))
                .sorted(Comparator.<Map.Entry<String, SourceMemoryEntry>, Long>comparing(
                        e -> e.getValue().lastAppliedTick()).reversed())
                .map(e -> {
                    try {
                        return ResourceLocation.parse(e.getKey());
                    } catch (Exception ex) {
                        MarieCore.LOGGER.warn("[MarieLib] Dropping invalid source key in history: '{}'", e.getKey());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean hasSourceRecently(ResourceLocation sourceId) {
        String key = sourceId.toString();
        SourceMemoryEntry entry = trackingData.sourceMemory.get(key);
        if (entry == null) return false;

        long halfLifeMs = IMarieConfig.get().memoryWindowMinutes() * 60_000L;
        long gameTimeMs = trackingData.lastTickTime > 0 ? trackingData.lastTickTime : System.currentTimeMillis();
        return !entry.isEffectivelyExpired(halfLifeMs, gameTimeMs, 0.1f);
    }

    @Override
    public long getTimeSinceSource(ResourceLocation sourceId) {
        String key = sourceId.toString();
        SourceMemoryEntry entry = trackingData.sourceMemory.get(key);
        if (entry == null) return -1L;

        long gameTimeMs = trackingData.lastTickTime > 0 ? trackingData.lastTickTime : System.currentTimeMillis();
        long elapsed = gameTimeMs - entry.lastAppliedTick();
        if (elapsed < 0) return -1L;

        // Convert ms to ticks (1 tick = 50ms)
        return elapsed / 50L;
    }
}
