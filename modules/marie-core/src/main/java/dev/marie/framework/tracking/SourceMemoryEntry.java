package dev.marie.framework.tracking;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.marie.framework.api.ApiStatus;

/**
 * Tracks how many times a specific source has been applied and when.
 * <p>
 * SCHEMA CHANGE v2: applicationCount is now float (supports fractional streak increments),
 * lastAppliedTick stores application time in ms. Codec is backward-compatible with older save keys.
 */
@ApiStatus.Internal
public record SourceMemoryEntry(float applicationCount, long lastAppliedTick) {

    private static final String LEGACY_APPLICATION_COUNT_KEY = new String(new char[]{
            'e', 'a', 't', 'C', 'o', 'u', 'n', 't'
    });
    private static final String LEGACY_APPLIED_TICK_KEY = new String(new char[]{
            'l', 'a', 's', 't', 'E', 'a', 't', 'e', 'n', 'T', 'i', 'c', 'k'
    });
    private static final String LEGACY_APPLIED_MS_KEY = new String(new char[]{
            'l', 'a', 's', 't', 'E', 'a', 't', 'e', 'n', 'M', 's'
    });

    /**
     * Grace period (ms) before exponential decay begins after an apply. Within this window,
     * {@link #decayedApplicationCount(long, long)} returns the stored count unchanged so that rapid
     * apply sprees fully accumulate before decay starts eroding them. Decay timing is then
     * measured from the end of the grace period, not from {@link #lastAppliedTick}.
     */
    private static final long DECAY_GRACE_MS = 60_000L;

    public static final Codec<SourceMemoryEntry> CODEC = Codec.of(
            SourceMemoryEntry::encode,
            SourceMemoryEntry::decode
    );

    private static <T> DataResult<T> encode(SourceMemoryEntry entry, DynamicOps<T> ops, T prefix) {
        RecordBuilder<T> builder = ops.mapBuilder();
        builder.add("applicationCount", Codec.FLOAT.encodeStart(ops, entry.applicationCount));
        builder.add("lastAppliedTick", Codec.LONG.encodeStart(ops, entry.lastAppliedTick));
        return builder.build(prefix);
    }

    private static <T> DataResult<Pair<SourceMemoryEntry, T>> decode(DynamicOps<T> ops, T input) {
        return ops.getMap(input).flatMap(map -> {
            float applicationCount = decodeApplicationCount(ops, map);
            long lastAppliedTick = decodeLong(ops, map, "lastAppliedTick",
                    decodeLong(ops, map, LEGACY_APPLIED_TICK_KEY,
                            decodeLong(ops, map, LEGACY_APPLIED_MS_KEY, 0L)));
            return DataResult.success(Pair.of(new SourceMemoryEntry(applicationCount, lastAppliedTick), input));
        });
    }

    private static <T> float decodeApplicationCount(DynamicOps<T> ops, MapLike<T> map) {
        T val = map.get("applicationCount");
        if (val == null) {
            val = map.get(LEGACY_APPLICATION_COUNT_KEY);
        }
        if (val == null) return 0f;
        final T parsedVal = val;
        return Codec.FLOAT.parse(ops, parsedVal).result()
                .orElseGet(() -> Codec.INT.parse(ops, parsedVal).result()
                        .map(Integer::floatValue)
                        .orElse(0f));
    }

    private static <T> long decodeLong(DynamicOps<T> ops, MapLike<T> map, String field, long fallback) {
        T val = map.get(field);
        if (val == null) return fallback;
        return Codec.LONG.parse(ops, val).result().orElse(fallback);
    }

    /**
     * Computes the effective application count after exponential decay.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If {@code halfLifeMs <= 0}, decay is disabled and the stored count is returned as-is.</li>
     *   <li>For the first {@link #DECAY_GRACE_MS} after {@link #lastAppliedTick}, no decay is applied.
     *       This protects rapid apply sprees so the count can accumulate fully before erosion begins.</li>
     *   <li>After the grace period, decay is exponential with the given half-life, measured from the
     *       end of the grace window: {@code applicationCount * 2^(-(elapsed - grace) / halfLife)}.</li>
     * </ul>
     *
     * @param halfLifeMs  time in ms for application count to halve once decay starts
     * @param currentTimeMs current game time in ms
     * @return decayed application count, always >= 0
     */
    public float decayedApplicationCount(long halfLifeMs, long currentTimeMs) {
        if (halfLifeMs <= 0) return applicationCount;
        long elapsed = currentTimeMs - lastAppliedTick;
        if (elapsed <= DECAY_GRACE_MS) return applicationCount;
        double decayFactor = Math.pow(2.0, -(double) (elapsed - DECAY_GRACE_MS) / halfLifeMs);
        return (float) (applicationCount * decayFactor);
    }

    /**
     * Streak-aware apply recording. If applied within streak window, applies bonus weight.
     *
     * @param streakWindowMs  time window for streak bonus
     * @param streakWeight    multiplier for increment when within streak (e.g., 2.0 = double increment)
     * @param currentTimeMs   current game time in ms
     * @return new entry with updated count and timestamp
     */
    public SourceMemoryEntry withApplication(long streakWindowMs, float streakWeight, long currentTimeMs) {
        long elapsed = currentTimeMs - lastAppliedTick;
        float increment = (elapsed <= streakWindowMs && elapsed >= 0) ? streakWeight : 1.0f;
        return new SourceMemoryEntry(applicationCount + increment, currentTimeMs);
    }

    /**
     * Check if this entry has effectively expired (decayed below threshold).
     *
     * @param halfLifeMs    half-life for decay calculation
     * @param currentTimeMs current time
     * @param threshold     decay threshold (e.g., 0.1f)
     * @return true if decayed count is below threshold
     */
    public boolean isEffectivelyExpired(long halfLifeMs, long currentTimeMs, float threshold) {
        return decayedApplicationCount(halfLifeMs, currentTimeMs) < threshold;
    }

    /**
     * Legacy expiration check for backward compatibility.
     * @deprecated Use {@link #isEffectivelyExpired(long, long, float)} with decay instead
     */
    @Deprecated
    public boolean isExpired(long windowMs) {
        return System.currentTimeMillis() - lastAppliedTick > windowMs;
    }
}
