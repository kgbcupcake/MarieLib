package dev.marie.MariesLib.tagaudit.model;

import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Context object passed to {@link dev.marie.MariesLib.tagaudit.rule.TagRule} instances
 * during a tag audit run. Provides access to tag contents, category membership,
 * and optionally a live inference lookup for cross-referencing.
 */
@ApiStatus.Stable
public interface TagAuditContext {

    /** All known tag category names for this mod (e.g. nutrient keys). */
    Set<String> knownCategories();

    /** All items assigned to the given category tag. */
    Set<ResourceLocation> itemsInCategory(String category);

    /** All categories the given item is tagged into. */
    Set<String> categoriesForItem(ResourceLocation itemId);

    /**
     * Optional live inference function: given an item id, returns a score map
     * independent of the tag data. Rules may use this to detect tag/inference
     * disagreements. Returns {@code null} if the context has no inference signal.
     */
    @Nullable
    Function<ResourceLocation, Map<String, Float>> liveInferenceLookup();

    /** All mod namespaces present in the item registry. */
    Set<String> namespacesPresent();
}
