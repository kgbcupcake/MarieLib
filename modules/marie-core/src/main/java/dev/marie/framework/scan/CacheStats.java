package dev.marie.framework.scan;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record CacheStats(
        int hits,
        int misses,
        int size,
        long avgResolveNanos,
        long slowestResolveNanos,
        @Nullable ResourceLocation slowestItem,
        int recipeTimeouts
) {}
