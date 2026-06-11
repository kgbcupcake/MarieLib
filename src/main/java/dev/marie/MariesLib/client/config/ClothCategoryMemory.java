package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigKeys;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

/** Memory / diminishing-returns Cloth entries for Marie mods that own gameplay balance config. */
public final class ClothCategoryMemory {

    private ClothCategoryMemory() {}

    public static void build(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        MariesLibConfigHolder h = ClothConfigHelper.holder();
        var cat = ClothConfigHelper.category(builder, "memory");

        ClothConfigHelper.addLong(cat, entryBuilder, MariesLibConfigKeys.MEMORY_WINDOW_MINUTES,
                "memory.memoryWindowMinutes", h.memoryWindowMinutes, 60L, 1L, Long.MAX_VALUE / 2,
                v -> h.memoryWindowMinutes = v);
        ClothConfigHelper.addInt(cat, entryBuilder, MariesLibConfigKeys.MEMORY_WINDOW_COUNT,
                "memory.memoryWindowCount", h.memoryWindowCount, 20, 1, Integer.MAX_VALUE,
                v -> h.memoryWindowCount = v);
        ClothConfigHelper.addLong(cat, entryBuilder, MariesLibConfigKeys.STREAK_WINDOW_MS,
                "memory.streakWindowMs", h.streakWindowMs, 300_000L, 0L, Long.MAX_VALUE / 2,
                v -> h.streakWindowMs = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.STREAK_WEIGHT,
                "memory.streakWeight", h.streakWeight, 1.5f, 0f, 10f, v -> h.streakWeight = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.DEBT_THRESHOLD,
                "memory.debtThreshold", h.debtThreshold, 5f, 0f, 100f, v -> h.debtThreshold = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.DEBT_DECAY_RATE,
                "memory.debtDecayRate", h.debtDecayRate, 0.01f, 0f, 1f, v -> h.debtDecayRate = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.DIMINISHING_STEEPNESS,
                "memory.diminishingSteepness", h.diminishingSteepness, 1f, 0.01f, 20f, v -> h.diminishingSteepness = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.DIMINISHING_MIDPOINT,
                "memory.diminishingMidpoint", h.diminishingMidpoint, 3f, 0.01f, 20f, v -> h.diminishingMidpoint = v);
        ClothConfigHelper.addDouble(cat, entryBuilder, MariesLibConfigKeys.NOVELTY_BONUS,
                "memory.noveltyBonus", h.noveltyBonus, 1.2, 0.0, 10.0, v -> h.noveltyBonus = v);
        ClothConfigHelper.addDouble(cat, entryBuilder, MariesLibConfigKeys.NOVELTY_DECAY_CAP,
                "memory.noveltyDecayCap", h.noveltyDecayCap, 3.0, 0.0, 100.0, v -> h.noveltyDecayCap = v);
        ClothConfigHelper.addDouble(cat, entryBuilder, MariesLibConfigKeys.DIMINISHING_FLOOR,
                "memory.diminishingFloor", h.diminishingFloor, 0.2, 0.0, 1.0, v -> h.diminishingFloor = v);
        ClothConfigHelper.addDouble(cat, entryBuilder, MariesLibConfigKeys.STARTING_VALUE_FILL,
                "memory.startingValueFill", h.startingValueFill, 0.5, 0.0, 1.0, v -> h.startingValueFill = v);
    }
}
