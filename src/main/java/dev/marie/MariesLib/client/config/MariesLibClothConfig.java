package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.client.ImportExportButtonsWidget;
import dev.marie.MariesLib.client.PresetsWidget;
import dev.marie.MariesLib.config.MariesLibConfigIO;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;

/**
 * Builds the MariesLib Cloth Config screen.
 */
public final class MariesLibClothConfig {

    private MariesLibClothConfig() {}

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(ClothConfigHelper.t("title"))
                .setSavingRunnable(MariesLibConfigIO::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ClothCategoryModules.build(builder, entryBuilder);
        ClothCategoryHandlers.build(builder, entryBuilder);
        ClothCategoryScanner.build(builder, entryBuilder);
        ClothCategoryMemory.build(builder, entryBuilder);
        ClothCategoryThresholds.build(builder, entryBuilder);
        ClothCategoryClient.build(builder, entryBuilder);
        ClothCategoryDebug.build(builder, entryBuilder);

        var presets = ClothConfigHelper.category(builder, "presets");
        presets.addEntry(new PresetsWidget(parent));
        presets.addEntry(new ImportExportButtonsWidget(parent));

        return builder.build();
    }
}
