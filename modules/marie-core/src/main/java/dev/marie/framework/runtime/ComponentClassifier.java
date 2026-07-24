package dev.marie.framework.runtime;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.cache.RunningAverage;
import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scan.StageContext;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stateless classification of an item purely as a component of something else (a recipe
 * ingredient, a crafting input, etc.) — separate from any resolver's primary eligibility gate,
 * cache, or learned state (namespace peers). Never writes to any resolver's cache; callers own
 * caching if they need it.
 *
 * <p>Deliberately excludes recipe-inheritance-style stages from the caller-supplied stage list —
 * this exists to classify a component in isolation, not to recurse into its own composition.</p>
 */
@ApiStatus.Experimental
public final class ComponentClassifier {

    private ComponentClassifier() {}

    /**
     * Runs {@code stages} in order against a fresh, disposable {@link StageContext} for
     * {@code stack}. Returns the first result with non-empty values, or {@code null} if no
     * stage produces one. Confidence/uncertainty filtering is the caller's responsibility —
     * {@link ResolutionResult} carries a raw {@code confidence} float but no uncertain() verdict.
     *
     * @param stack          the item to classify
     * @param recipeManager  nullable; passed through to {@link StageContext} for stages that need it
     * @param validKeys      the set of value keys this classification may target
     * @param stages         ordered stages to attempt, in order — should not include any
     *                       recipe-inheritance-style stage
     */
    public static @Nullable ResolutionResult classify(
            ItemStack stack,
            @Nullable RecipeManager recipeManager,
            Set<String> validKeys,
            List<ResolutionStageHandler> stages
    ) {
        if (stack.isEmpty() || stack.getItem() == null) {
            return null;
        }
        Item item = stack.getItem();
        ResourceLocation itemId = item.builtInRegistryHolder().key().location();
        Holder<Item> holder = stack.getItemHolder();

        // Fresh, disposable per-call state — never shared with any resolver's persistent maps.
        Map<String, RunningAverage> scratchNamespacePeers = new ConcurrentHashMap<>();
        StageContext ctx = new StageContext(holder, itemId, recipeManager, scratchNamespacePeers, validKeys, null);

        for (ResolutionStageHandler stage : stages) {
            ResolutionResult result = stage.resolve(itemId, ctx);
            if (result != null && !result.values().isEmpty()) {
                return result;
            }
        }
        return null;
    }
}
