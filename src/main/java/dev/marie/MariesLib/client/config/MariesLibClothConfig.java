package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.MariesLibConfigIO;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;

/**
 * Builds the MariesLib Cloth Config screen with framework-only tabs:
 * Overview, Scanner, Diagnostics, and Tools.
 */
public final class MariesLibClothConfig {

    private MariesLibClothConfig() {}

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(ClothConfigHelper.t("title"))
                .setSavingRunnable(MariesLibConfigIO::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ClothCategoryOverview.build(builder, entryBuilder);
        ClothCategoryScanner.build(builder, entryBuilder, parent);
        ClothCategoryDiagnostics.build(builder, entryBuilder);
        ClothCategoryTools.build(builder, entryBuilder);

        return builder.build();
    }
}
