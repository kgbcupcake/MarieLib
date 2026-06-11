package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.marie.MariesLib.tracking.TrackingMemoryConfig;

final class HandlerSupport {

    private HandlerSupport() {}

    static TrackingMemoryConfig resolveMemoryConfig() {
        return IMarieLibConfig.get().trackingMemoryConfig();
    }

    static void resetMemoryConfigWarning() {
    }
}
