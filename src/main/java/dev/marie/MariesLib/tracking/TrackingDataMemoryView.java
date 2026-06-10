package dev.marie.MariesLib.tracking;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MemoryView;
import dev.marie.MariesLib.core.MarieLibContext;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Concrete implementation of {@link MemoryView} backed by a player's {@link TrackingData}.
 */
@ApiStatus.Internal
public final class TrackingDataMemoryView implements MemoryView {

    private final TrackingData trackingData;

    public TrackingDataMemoryView(TrackingData trackingData) {
        this.trackingData = trackingData;
    }

    @Override
    public List<ResourceLocation> getRecentSources() {
        long halfLifeMs = MarieLibContext.get().memoryWindowMinutes() * 60_000L;
        long gameTimeMs = trackingData.lastTickTime > 0 ? trackingData.lastTickTime : System.currentTimeMillis();

        return trackingData.sourceMemory.entrySet().stream()
                .filter(e -> !e.getValue().isEffectivelyExpired(halfLifeMs, gameTimeMs, 0.1f))
                .sorted(Comparator.<Map.Entry<String, SourceMemoryEntry>, Long>comparing(
                        e -> e.getValue().lastAppliedTick()).reversed())
                .map(e -> ResourceLocation.parse(e.getKey()))
                .toList();
    }

    @Override
    public boolean hasSourceRecently(ResourceLocation sourceId) {
        String key = sourceId.toString();
        SourceMemoryEntry entry = trackingData.sourceMemory.get(key);
        if (entry == null) return false;

        long halfLifeMs = MarieLibContext.get().memoryWindowMinutes() * 60_000L;
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
