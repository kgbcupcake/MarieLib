package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.marie.MariesLib.tracking.DiminishingReturnsConfig;

final class DiminishingReturnsSupport {

    private DiminishingReturnsSupport() {}

    static DiminishingReturnsConfig resolveMemoryConfig() {
        return IMarieLibConfig.get().trackingMemoryConfig();
    }

    static void resetMemoryConfigWarning() {
    }
}
