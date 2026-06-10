package dev.marie.MariesLib.tracking;

public record TrackingMemoryConfig(
    long memoryWindowMinutes,
    double noveltyBonus,
    double noveltyDecayCap,
    double diminishingFloor,
    double startingValueFill
) {}
