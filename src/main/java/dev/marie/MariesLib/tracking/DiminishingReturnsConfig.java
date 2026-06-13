package dev.marie.MariesLib.tracking;

import dev.marie.MariesLib.api.ApiStatus;

@ApiStatus.Internal
public record DiminishingReturnsConfig(
    long memoryWindowMinutes,
    double noveltyBonus,
    double noveltyDecayCap,
    double diminishingFloor,
    double startingValueFill
) {}
