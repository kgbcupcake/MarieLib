package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigKeys;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

final class ClothCategoryDiagnostics {

    private ClothCategoryDiagnostics() {}

    static void build(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        MariesLibConfigHolder h = ClothConfigHelper.holder();
        var cat = ClothConfigHelper.category(builder, "diagnostics");

        cat.addEntry(ClothConfigHelper.buildBool(entryBuilder, MariesLibConfigKeys.ENABLE_DEBUG_LOGGING,
                "diagnostics.enableDebugLogging", h.enableDebugLogging, false,
                v -> h.enableDebugLogging = v));

        cat.addEntry(entryBuilder.startTextDescription(ClothConfigHelper.t("diagnostics.hint")).build());
    }
}
