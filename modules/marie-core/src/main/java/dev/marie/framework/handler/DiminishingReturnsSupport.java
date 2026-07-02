package dev.marie.framework.handler;

import dev.marie.framework.core.IMarieLibConfig;
import dev.marie.framework.tracking.DiminishingReturnsConfig;

final class DiminishingReturnsSupport {

    private DiminishingReturnsSupport() {}

    static DiminishingReturnsConfig resolveMemoryConfig() {
        return IMarieLibConfig.get().trackingMemoryConfig();
    }

    static void resetMemoryConfigWarning() {
    }
}
