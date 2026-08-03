package dev.marie.framework.tracking.tracker.definition;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Defines a custom tracker that accumulates a float value per player over a bounded period,
 * retaining a capped history of completed periods.
 *
 * <p>Use the static factories for common periods, or {@link #builder(ResourceLocation, TrackerPeriod)}
 * for {@link TrackerPeriod#REAL_TIME} or {@link TrackerPeriod#CUSTOM} trackers.</p>
 *
 * <p>Register via {@code MarieTracking.registerTracker}, idempotently — every {@code /reload}
 * wipes the tracker registry, so re-register from {@code MarieContext.reloadBroadcastHook()} to
 * survive it.</p>
 */
@ApiStatus.Stable
public final class TrackerDefinition {

    private final ResourceLocation id;
    private final TrackerPeriod period;
    private final int retention;
    private final long realTimeDurationMs;
    private final boolean immediateSync;

    private TrackerDefinition(ResourceLocation id, TrackerPeriod period, int retention, long realTimeDurationMs,
            boolean immediateSync) {
        this.id = id;
        this.period = period;
        this.retention = retention;
        this.realTimeDurationMs = realTimeDurationMs;
        this.immediateSync = immediateSync;
    }

    @ApiStatus.Stable
    public ResourceLocation getId() {
        return id;
    }

    @ApiStatus.Stable
    public TrackerPeriod getPeriod() {
        return period;
    }

    /**
     * Maximum number of completed periods retained in history. Validated against
     * {@code IMarieConfig#trackerMaxRetention()} at registration time, not here.
     */
    @ApiStatus.Stable
    public int getRetention() {
        return retention;
    }

    /**
     * Real-time period duration in milliseconds. Only meaningful for {@link TrackerPeriod#REAL_TIME}.
     */
    @ApiStatus.Stable
    public long getRealTimeDurationMs() {
        return realTimeDurationMs;
    }

    /**
     * Whether this tracker bypasses the throttled dirty-flag sweep and instead syncs its live
     * value to the client on every single {@code incrementTracker} call.
     */
    @ApiStatus.Stable
    public boolean isImmediateSync() {
        return immediateSync;
    }

    @ApiStatus.Stable
    public static TrackerDefinition daily(ResourceLocation id, int retention) {
        return builder(id, TrackerPeriod.DAILY).retention(retention).build();
    }

    @ApiStatus.Stable
    public static TrackerDefinition weekly(ResourceLocation id, int retention) {
        return builder(id, TrackerPeriod.WEEKLY).retention(retention).build();
    }

    @ApiStatus.Stable
    public static TrackerDefinition monthly(ResourceLocation id, int retention) {
        return builder(id, TrackerPeriod.MONTHLY).retention(retention).build();
    }

    @ApiStatus.Stable
    public static Builder builder(ResourceLocation id, TrackerPeriod period) {
        return new Builder(id, period);
    }

    @ApiStatus.Stable
    public static final class Builder {
        private final ResourceLocation id;
        private final TrackerPeriod period;
        private int retention = -1;
        private long realTimeDurationMs = 0L;
        private boolean immediateSync = false;

        private Builder(ResourceLocation id, TrackerPeriod period) {
            this.id = Objects.requireNonNull(id, "id");
            this.period = Objects.requireNonNull(period, "period");
        }

        @ApiStatus.Stable
        public Builder retention(int retention) {
            this.retention = retention;
            return this;
        }

        /**
         * Sets the real-time period duration. Required when {@code period} is {@link TrackerPeriod#REAL_TIME}.
         */
        @ApiStatus.Stable
        public Builder realTimeDuration(long amount, TimeUnit unit) {
            this.realTimeDurationMs = unit.toMillis(amount);
            return this;
        }

        /**
         * Opts this tracker into immediate sync: {@link dev.marie.framework.tracking.tracker.MarieTracking#incrementTracker}
         * pushes the tracker's updated value to the client on every single call, bypassing the
         * throttled dirty-flag sweep ({@code IMarieConfig#trackerSyncIntervalTicks()}) entirely
         * for this tracker.
         *
         * <p>Intended for low-frequency, latency-sensitive trackers — e.g. a UI element that must
         * feel instant. <strong>Not</strong> intended for high-frequency trackers (mining, combat,
         * anything that could fire many times per second): those will flood the network exactly
         * the way the throttle exists to prevent. The size/rate limits already enforced in
         * {@code MarieNetworking} (4096 byte cap, 20 packets/sec) still apply and will silently
         * drop excess traffic if this is misused, but that's a safety net, not a substitute for
         * choosing the right mode.</p>
         *
         * <p>Not exposed via {@link TrackerDefinition#daily}, {@link TrackerDefinition#weekly}, or
         * {@link TrackerDefinition#monthly} — those stay throttled-only convenience methods. This
         * is a deliberate opt-in reachable only through the full builder.</p>
         */
        @ApiStatus.Stable
        public Builder immediateSync(boolean immediateSync) {
            this.immediateSync = immediateSync;
            return this;
        }

        @ApiStatus.Stable
        public TrackerDefinition build() {
            if (period == TrackerPeriod.REAL_TIME && realTimeDurationMs <= 0L) {
                throw new IllegalStateException(
                        "TrackerDefinition '" + id + "': REAL_TIME period requires realTimeDuration(...)");
            }
            return new TrackerDefinition(id, period, retention, realTimeDurationMs, immediateSync);
        }
    }
}
