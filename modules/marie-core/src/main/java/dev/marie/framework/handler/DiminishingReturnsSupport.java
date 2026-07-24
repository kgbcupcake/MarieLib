package dev.marie.framework.handler;

import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.tracking.DiminishingReturnsConfig;

final class DiminishingReturnsSupport {

    private DiminishingReturnsSupport() {}

    static DiminishingReturnsConfig resolveMemoryConfig() {
        return IMarieConfig.get().trackingMemoryConfig();
    }

    static void resetMemoryConfigWarning() {
    }
}
