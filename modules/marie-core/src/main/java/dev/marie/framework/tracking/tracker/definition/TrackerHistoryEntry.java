package dev.marie.framework.tracking.tracker.definition;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

/**
 * A single completed accumulation period for a tracker.
 *
 * @param trackerId     the tracker this entry belongs to
 * @param periodId      {@link TrackerPeriod#configId()} of the tracker at the time the period closed
 * @param startBoundary period start, in the time domain of {@code periodId} (game time ticks, or epoch ms for REAL_TIME)
 * @param endBoundary   period end, same time domain as {@code startBoundary}
 * @param value         the accumulated value when the period closed
 */
@ApiStatus.Stable
public record TrackerHistoryEntry(
        ResourceLocation trackerId,
        String periodId,
        long startBoundary,
        long endBoundary,
        float value
) {

    public static final Codec<TrackerHistoryEntry> CODEC = Codec.of(
            TrackerHistoryEntry::encode,
            TrackerHistoryEntry::decode
    );

    private static <T> DataResult<T> encode(TrackerHistoryEntry entry, DynamicOps<T> ops, T prefix) {
        RecordBuilder<T> builder = ops.mapBuilder();
        builder.add("tracker_id", ResourceLocation.CODEC.encodeStart(ops, entry.trackerId));
        builder.add("period_id", Codec.STRING.encodeStart(ops, entry.periodId));
        builder.add("start_boundary", Codec.LONG.encodeStart(ops, entry.startBoundary));
        builder.add("end_boundary", Codec.LONG.encodeStart(ops, entry.endBoundary));
        builder.add("value", Codec.FLOAT.encodeStart(ops, entry.value));
        return builder.build(prefix);
    }

    private static <T> DataResult<Pair<TrackerHistoryEntry, T>> decode(DynamicOps<T> ops, T input) {
        return ops.getMap(input).flatMap(map -> {
            ResourceLocation trackerId = decode(ops, map, "tracker_id", ResourceLocation.CODEC,
                    ResourceLocation.withDefaultNamespace("unknown"));
            String periodId = decode(ops, map, "period_id", Codec.STRING, "daily");
            long startBoundary = decode(ops, map, "start_boundary", Codec.LONG, 0L);
            long endBoundary = decode(ops, map, "end_boundary", Codec.LONG, 0L);
            float value = decode(ops, map, "value", Codec.FLOAT, 0f);
            return DataResult.success(Pair.of(
                    new TrackerHistoryEntry(trackerId, periodId, startBoundary, endBoundary, value), input));
        });
    }

    private static <T, A> A decode(DynamicOps<T> ops, MapLike<T> map, String field, Codec<A> codec, A fallback) {
        T val = map.get(field);
        if (val == null) return fallback;
        return codec.parse(ops, val).result().orElse(fallback);
    }
}
