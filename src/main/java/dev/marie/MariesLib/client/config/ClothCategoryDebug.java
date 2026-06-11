package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigKeys;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

final class ClothCategoryDebug {

    private ClothCategoryDebug() {}

    static void build(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        MariesLibConfigHolder h = ClothConfigHelper.holder();
        var cat = ClothConfigHelper.category(builder, "debug");

        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_DEBUG_LOGGING,
                "debug.enableDebugLogging", h.enableDebugLogging, false, v -> h.enableDebugLogging = v);
    }
}
