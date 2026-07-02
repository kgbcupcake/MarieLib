package dev.marie.framework.tracking;

import dev.marie.framework.api.ApiStatus;

@ApiStatus.Internal
public record DiminishingReturnsConfig(
    long memoryWindowMinutes,
    double noveltyBonus,
    double noveltyDecayCap,
    double diminishingFloor,
    double startingValueFill
) {}
