package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigKeys;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

final class ClothCategoryThresholds {

    private ClothCategoryThresholds() {}

    static void build(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        MariesLibConfigHolder h = ClothConfigHelper.holder();
        var cat = ClothConfigHelper.category(builder, "thresholds");

        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.EXCESS_THRESHOLD,
                "thresholds.excessThreshold", h.excessThreshold, 0.9f, 0f, 1f, v -> h.excessThreshold = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.LOW_THRESHOLD,
                "thresholds.lowThreshold", h.lowThreshold, 0.3f, 0f, 1f, v -> h.lowThreshold = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.CRITICAL_THRESHOLD,
                "thresholds.criticalThreshold", h.criticalThreshold, 0.25f, 0f, 1f, v -> h.criticalThreshold = v);
        ClothConfigHelper.addInt(cat, entryBuilder, MariesLibConfigKeys.DECAY_INTERVAL_TICKS,
                "thresholds.decayIntervalTicks", h.decayIntervalTicks, 20, 1, 200,
                v -> h.decayIntervalTicks = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.DEFAULT_DECAY_RATE,
                "thresholds.defaultDecayRate", h.defaultDecayRate, 0f, 0f, 1f, v -> h.defaultDecayRate = v);
    }
}
