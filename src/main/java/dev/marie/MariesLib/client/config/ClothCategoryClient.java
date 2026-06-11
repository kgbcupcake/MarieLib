package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigKeys;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

final class ClothCategoryClient {

    private ClothCategoryClient() {}

    static void build(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        MariesLibConfigHolder h = ClothConfigHelper.holder();
        var cat = ClothConfigHelper.category(builder, "client");

        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.SHOW_JOIN_MESSAGE,
                MariesLibConfigKeys.SHOW_JOIN_MESSAGE, h.showJoinMessage, false, v -> h.showJoinMessage = v);
    }
}
