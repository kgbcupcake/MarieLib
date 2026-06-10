package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.tracking.TrackingMemoryConfig;

import java.util.concurrent.atomic.AtomicBoolean;

final class HandlerSupport {

    private static final AtomicBoolean MEMORY_CONFIG_WARN_ONCE = new AtomicBoolean(false);

    private HandlerSupport() {}

    static TrackingMemoryConfig resolveMemoryConfig() {
        TrackingMemoryConfig fromProvider = MarieLibContext.get().trackingMemoryConfigProvider().get();
        if (fromProvider != null) {
            return fromProvider;
        }
        if (MEMORY_CONFIG_WARN_ONCE.compareAndSet(false, true)) {
            MariesLib.LOGGER.warn(
                    "[MarieLib] trackingMemoryConfigProvider returned null, using context defaults. Will not warn again until server restart.");
        }
        var ctx = MarieLibContext.get();
        return new TrackingMemoryConfig(
                ctx.memoryWindowMinutes(),
                1.2,
                3.0,
                0.2,
                0.5);
    }

    static void resetMemoryConfigWarning() {
        MEMORY_CONFIG_WARN_ONCE.set(false);
    }
}
