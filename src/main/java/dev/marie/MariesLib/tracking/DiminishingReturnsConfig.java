package dev.marie.MariesLib.tracking;

public record DiminishingReturnsConfig(
    long memoryWindowMinutes,
    double noveltyBonus,
    double noveltyDecayCap,
    double diminishingFloor,
    double startingValueFill
) {}
