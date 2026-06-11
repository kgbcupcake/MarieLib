package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigKeys;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

final class ClothCategoryHandlers {

    private ClothCategoryHandlers() {}

    static void build(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        MariesLibConfigHolder h = ClothConfigHelper.holder();
        var cat = ClothConfigHelper.category(builder, "handlers");

        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_DECAY,
                "handlers.enableDecay", h.enableDecay, true, v -> h.enableDecay = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_SOURCE_APPLICATION,
                "handlers.enableSourceApplication", h.enableSourceApplication, true, v -> h.enableSourceApplication = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_EFFECTS,
                "handlers.enableEffects", h.enableEffects, true, v -> h.enableEffects = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_SLEEP_BONUS,
                "handlers.enableSleepBonus", h.enableSleepBonus, true, v -> h.enableSleepBonus = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_BLOCK_HEAVY_SOURCES,
                "handlers.enableBlockHeavySources", h.enableBlockHeavySources, false, v -> h.enableBlockHeavySources = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_BLOCK_LIGHT_SOURCE,
                "handlers.enableBlockLightSource", h.enableBlockLightSource, false, v -> h.enableBlockLightSource = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_TOASTS,
                "handlers.enableToasts", h.enableToasts, true, v -> h.enableToasts = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_CRITICAL_TOASTS,
                "handlers.enableCriticalToasts", h.enableCriticalToasts, true, v -> h.enableCriticalToasts = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_SOURCE_TOOLTIPS,
                "handlers.enableSourceTooltips", h.enableSourceTooltips, true, v -> h.enableSourceTooltips = v);
        ClothConfigHelper.addInt(cat, entryBuilder, MariesLibConfigKeys.DEFAULT_EFFECT_DURATION_TICKS,
                "handlers.defaultEffectDurationTicks", h.defaultEffectDurationTicks, 140, 1, 72000,
                v -> h.defaultEffectDurationTicks = v);
    }
}
