package dev.marie.MariesLib.client.config;

import java.util.List;
import java.util.stream.Collectors;

import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MarieModRegistry;
import dev.marie.MariesLib.core.MariesLib;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/**
 * Overview tab: read-only framework status information.
 */
final class ClothCategoryOverview {

    private ClothCategoryOverview() {}

    static void build(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory cat = ClothConfigHelper.category(builder, "overview");

        String version = ModList.get()
                .getModContainerById(MariesLib.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");

        cat.addEntry(entryBuilder.startTextDescription(
                Component.translatable(ClothConfigHelper.key("overview.version"), version)
        ).build());

        cat.addEntry(entryBuilder.startTextDescription(
                Component.translatable(ClothConfigHelper.key("overview.configPath"), "config/marieslib.cfg")
        ).build());

        List<MarieLibContext> mods = MarieModRegistry.getAll();
        int modCount = mods.size();
        String modNames = mods.isEmpty()
                ? Component.translatable(ClothConfigHelper.key("overview.noMods")).getString()
                : mods.stream().map(MarieLibContext::modId).collect(Collectors.joining(", "));

        cat.addEntry(entryBuilder.startTextDescription(
                Component.translatable(ClothConfigHelper.key("overview.registeredMods"), modCount + " (" + modNames + ")")
        ).build());

        MarieLibContext primary = MarieModRegistry.getPrimaryOrNull();
        String primaryName = primary != null ? primary.modId() : "none";
        cat.addEntry(entryBuilder.startTextDescription(
                Component.translatable(ClothConfigHelper.key("overview.primaryMod"), primaryName)
        ).build());

        int valueCount = ValueRegistry.getAll().size();
        cat.addEntry(entryBuilder.startTextDescription(
                Component.translatable(ClothConfigHelper.key("overview.registeredValues"), valueCount)
        ).build());

        cat.addEntry(entryBuilder.startTextDescription(
                ClothConfigHelper.t("overview.gameplayHint")
        ).build());
    }
}
