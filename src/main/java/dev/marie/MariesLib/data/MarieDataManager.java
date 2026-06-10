package dev.marie.MariesLib.data;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.Set;

/**
 * Registers and exposes the MarieLib datapack reload state.
 */
public final class MarieDataManager {

    private static final MarieDataLoader LOADER = new MarieDataLoader();

    private MarieDataManager() {}

    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(LOADER);
    }

    public static Set<ResourceLocation> getLoadedValues() {
        return LOADER.getLoadedValues();
    }

    public static Set<ResourceLocation> getLoadedSourceClassifications() {
        return LOADER.getLoadedSourceClassifications();
    }

    public static Set<ResourceLocation> getLoadedEffects() {
        return LOADER.getLoadedEffects();
    }

    public static Set<ResourceLocation> getLoadedSynergies() {
        return LOADER.getLoadedSynergies();
    }

    public static Set<ResourceLocation> getLoadedSourcePairSynergies() {
        return LOADER.getLoadedSourcePairSynergies();
    }

    public static Set<ResourceLocation> getLoadedMilestones() {
        return LOADER.getLoadedMilestones();
    }

    public static Set<ResourceLocation> getLoadedProfiles() {
        return LOADER.getLoadedProfiles();
    }

    public static Set<ResourceLocation> getLoadedCompatEntries() {
        return LOADER.getLoadedCompatEntries();
    }
}
