package dev.marie.MariesLib.tagaudit.model;

import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Read-only access a {@link dev.marie.MariesLib.tagaudit.rule.TagRule} needs
 * to evaluate items. Reusable infrastructure — implemented by the consuming
 * mod, which supplies real access to its own tag files, namespace data, and
 * inference pipeline. MarieLib defines only the shape of this access, not
 * what's behind it.
 */
@ApiStatus.Stable
public interface TagAuditContext {

    /**
     * All tag-file categories currently known (e.g. "proteins", "fruits", "grains"
     * for Nourished). The consuming mod defines what these strings mean.
     */
    Set<String> knownCategories();

    /**
     * Every item id currently tagged into the given category, per the bundled/
     * datapack tag data as the consuming mod sees it today (NOT live inference —
     * this is "what the tag file currently says").
     */
    Set<ResourceLocation> itemsInCategory(String category);

    /**
     * The category (or categories) a given item is currently tagged into, if any.
     * Empty if the item has no tag in any known category.
     */
    Set<String> categoriesForItem(ResourceLocation itemId);

    /**
     * Optional: a live-inference callback, if the consuming mod wants to let rules
     * compare "what the tag says" against "what live runtime inference would say
     * today." Returns null if no live-inference comparison is available/wired by
     * the consuming mod for this context. A rule should treat a null result here
     * as "this signal isn't available" and fall back to other signals, not as an error.
     */
    @Nullable
    Function<ResourceLocation, Map<String, Float>> liveInferenceLookup();

    /**
     * The mod namespace for each item id currently known to the consuming mod's
     * item universe — exposed separately from itemsInCategory/categoriesForItem
     * since namespace-bias rules need this even for items not yet in any category.
     */
    Set<String> namespacesPresent();
}
