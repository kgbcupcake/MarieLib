package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigKeys;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

final class ClothCategoryModules {

    private ClothCategoryModules() {}

    static void build(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        MariesLibConfigHolder h = ClothConfigHelper.holder();
        var cat = ClothConfigHelper.category(builder, "modules");

        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_SYNERGIES,
                MariesLibConfigKeys.ENABLE_SYNERGIES, h.enableSynergies, true, v -> h.enableSynergies = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_MILESTONES,
                MariesLibConfigKeys.ENABLE_MILESTONES, h.enableMilestones, true, v -> h.enableMilestones = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_SEASON_HOOKS,
                MariesLibConfigKeys.ENABLE_SEASON_HOOKS, h.enableSeasonHooks, true, v -> h.enableSeasonHooks = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_ABSORPTION_MODIFIERS,
                MariesLibConfigKeys.ENABLE_ABSORPTION_MODIFIERS, h.enableAbsorptionModifiers, true, v -> h.enableAbsorptionModifiers = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_TOTAL_TRACKING,
                MariesLibConfigKeys.ENABLE_TOTAL_TRACKING, h.enableTotalTracking, true, v -> h.enableTotalTracking = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_HUD,
                MariesLibConfigKeys.ENABLE_HUD, h.enableHUD, true, v -> h.enableHUD = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.ENABLE_TRACKING_SCREEN,
                MariesLibConfigKeys.ENABLE_TRACKING_SCREEN, h.enableTrackingScreen, true, v -> h.enableTrackingScreen = v);
    }
}
