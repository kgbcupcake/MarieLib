package dev.marie.framework.runtime;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.scan.CacheStats;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cache-hit/miss counters and resolve-timing telemetry for {@link RuntimeResolver}, extracted
 * to keep resolution logic and instrumentation independently readable. Owned exclusively by
 * {@link RuntimeResolver} via composition — not intended to be shared or held elsewhere.
 */
@ApiStatus.Internal
final class RuntimeResolverStats {

    private final AtomicInteger cacheHits = new AtomicInteger();
    private final AtomicInteger cacheMisses = new AtomicInteger();
    private final AtomicLong totalResolveNanos = new AtomicLong(0);
    private final AtomicLong slowestResolveNanos = new AtomicLong(0);
    private final AtomicReference<ResourceLocation> slowestItem = new AtomicReference<>(null);
    private final AtomicInteger recipeTimeouts = new AtomicInteger(0);

    void recordHit() {
        cacheHits.incrementAndGet();
    }

    void recordMiss() {
        cacheMisses.incrementAndGet();
    }

    void recordTiming(long elapsedNanos, ResourceLocation itemId) {
        totalResolveNanos.addAndGet(elapsedNanos);
        for (; ; ) {
            long prev = slowestResolveNanos.get();
            if (elapsedNanos < prev) {
                break;
            }
            if (slowestResolveNanos.compareAndSet(prev, elapsedNanos)) {
                slowestItem.set(itemId);
                break;
            }
        }
    }

    void recordRecipeTimeout() {
        recipeTimeouts.incrementAndGet();
    }

    void reset() {
        totalResolveNanos.set(0);
        slowestResolveNanos.set(0);
        slowestItem.set(null);
        recipeTimeouts.set(0);
    }

    CacheStats getCacheStats(int cacheSize) {
        int total = cacheMisses.get();
        long avg = total == 0 ? 0L : totalResolveNanos.get() / total;
        return new CacheStats(
                cacheHits.get(),
                cacheMisses.get(),
                cacheSize,
                avg,
                slowestResolveNanos.get(),
                slowestItem.get(),
                recipeTimeouts.get()
        );
    }

    static float computeSpread(Map<String, Float> scores) {
        float first = Float.NEGATIVE_INFINITY;
        float second = Float.NEGATIVE_INFINITY;
        for (float v : scores.values()) {
            if (v > first) {
                second = first;
                first = v;
            } else if (v > second) {
                second = v;
            }
        }
        if (first == Float.NEGATIVE_INFINITY) return 0f;
        if (second == Float.NEGATIVE_INFINITY) return first;
        return first - second;
    }
}
