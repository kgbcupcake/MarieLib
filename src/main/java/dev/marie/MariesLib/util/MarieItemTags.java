package dev.marie.MariesLib.util;

import dev.marie.MariesLib.core.MarieLibContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Shared item tags used by client and server logic.
 */
public final class MarieItemTags {

    private MarieItemTags() {}

    public static TagKey<Item> heavySource() {
        return MarieRegistryUtils.itemTagKey(MarieLibContext.get().modId() + ":heavy_source");
    }

    public static TagKey<Item> lightSource() {
        return MarieRegistryUtils.itemTagKey(MarieLibContext.get().modId() + ":light_source");
    }

    public static TagKey<Item> rawSourceFine() {
        return MarieRegistryUtils.itemTagKey(MarieLibContext.get().modId() + ":raw_source/fine");
    }

    public static TagKey<Item> rawSourceMild() {
        return MarieRegistryUtils.itemTagKey(MarieLibContext.get().modId() + ":raw_source/mild");
    }

    public static TagKey<Item> rawSourceMedium() {
        return MarieRegistryUtils.itemTagKey(MarieLibContext.get().modId() + ":raw_source/medium");
    }

    public static TagKey<Item> rawSourceSevere() {
        return MarieRegistryUtils.itemTagKey(MarieLibContext.get().modId() + ":raw_source/severe");
    }
}
