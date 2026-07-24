package dev.marie.framework.scanner.stages;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.runtime.ComponentClassifier;
import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scanner.ClassificationResult;
import dev.marie.framework.scanner.ClassificationSignal;
import dev.marie.framework.scanner.RecipeInheritanceResolver;
import dev.marie.framework.scanner.ScannerSpecRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Scanner signal stage: inherits value scores from recipe ingredients.
 * Tag-classified targets skip entirely; authoritative tag keys on the target are never overridden.
 */
@ApiStatus.Internal
public final class RecipeInheritanceStage {

    private RecipeInheritanceStage() {}

    /**
     * Resolves recipe inheritance and merges qualifying values into {@code scores}.
     *
     * @param valueTagScoresProvider function mapping an item to its value tag scores
     */
    public static void apply(
            Item item,
            ResourceLocation itemId,
            Map<String, Float> scores,
            List<ClassificationSignal> signals,
            boolean tagClassified,
            @Nullable RecipeInheritanceResolver recipeResolver,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            float multiplier,
            Function<Item, Map<String, Float>> valueTagScoresProvider,
            Set<String> validKeys,
            List<ResolutionStageHandler> componentFallbackStages
    ) {
        if (tagClassified || recipeResolver == null) {
            return;
        }

        Set<String> authoritativeKeys = valueTagScoresProvider.apply(item).keySet();
        Function<ResourceLocation, ClassificationResult> lookup =
                buildLookup(classifiedLookup, valueTagScoresProvider, validKeys, componentFallbackStages);

        Map<String, Float> recipeContribs = recipeResolver.resolve(item, lookup);
        if (recipeContribs.isEmpty()) {
            return;
        }

        Map<String, Float> scaled = mergeQualifyingContributions(scores, recipeContribs, authoritativeKeys, multiplier);

        if (!scaled.isEmpty()) {
            signals.add(new ClassificationSignal(
                    ClassificationSignal.TYPE_RECIPE_INHERITANCE,
                    "recipe_ingredients",
                    scaled
            ));
        }
    }

    /**
     * Merges authoritative {@code tagScores} with qualifying recipe-inherited secondary scores.
     * Tag keys are never overridden by recipe inheritance. Used for already-tagged items, where
     * {@link #apply} skips entirely (its {@code tagClassified} guard is unchanged).
     *
     * <p>{@code classifiedLookup} is widened with the same tag-score fallback {@link #buildLookup}
     * uses (see {@link #tagScoreFallbackLookup}): ingredients not yet present in
     * {@code classifiedLookup} (e.g. not yet visited in a same-pass registry iteration) are
     * queried directly via {@code valueTagScoresProvider} before being treated as unclassified.
     *
     * @return a new map: {@code tagScores} plus any qualifying recipe-inherited secondaries
     */
    public static Map<String, Float> mergeTagAndRecipeScores(
            Item item,
            Map<String, Float> tagScores,
            @Nullable RecipeInheritanceResolver recipeResolver,
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            Function<Item, Map<String, Float>> valueTagScoresProvider
    ) {
        Map<String, Float> merged = new HashMap<>(tagScores);
        if (recipeResolver == null) {
            return merged;
        }

        Function<ResourceLocation, ClassificationResult> lookup =
                tagScoreFallbackLookup(classifiedLookup, valueTagScoresProvider);

        Map<String, Float> recipeContribs = recipeResolver.resolve(item, lookup);
        if (recipeContribs.isEmpty()) {
            return merged;
        }

        float multiplier = ScannerSpecRegistry.get().multipliers().recipeInheritance();
        mergeQualifyingContributions(merged, recipeContribs, tagScores.keySet(), multiplier);
        return merged;
    }

    /**
     * Merges {@code recipeContribs} into {@code scores}, skipping any key present in
     * {@code authoritativeKeys} and only filling keys that aren't already positively scored.
     *
     * @return the scaled contributions actually merged in (empty if none qualified)
     */
    private static Map<String, Float> mergeQualifyingContributions(
            Map<String, Float> scores,
            Map<String, Float> recipeContribs,
            Set<String> authoritativeKeys,
            float multiplier
    ) {
        Map<String, Float> scaled = new HashMap<>();
        for (Map.Entry<String, Float> e : recipeContribs.entrySet()) {
            String key = e.getKey();
            if (authoritativeKeys.contains(key)) {
                continue;
            }
            if (scores.getOrDefault(key, 0f) <= 0f) {
                float value = e.getValue() * multiplier;
                scores.merge(key, value, Float::sum);
                scaled.put(key, value);
            }
        }
        return scaled;
    }

    private static Function<ResourceLocation, ClassificationResult> buildLookup(
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            Function<Item, Map<String, Float>> valueTagScoresProvider,
            Set<String> validKeys,
            List<ResolutionStageHandler> componentFallbackStages
    ) {
        Function<ResourceLocation, ClassificationResult> tagFallback =
                tagScoreFallbackLookup(classifiedLookup, valueTagScoresProvider);
        return id -> {
            ClassificationResult viaTag = tagFallback.apply(id);
            if (viaTag != null) {
                return viaTag;
            }
            Item ingredient = BuiltInRegistries.ITEM.get(id);
            if (ingredient == null) {
                return null;
            }
            ResolutionResult componentResult = ComponentClassifier.classify(
                    new ItemStack(ingredient), null, validKeys, componentFallbackStages);
            if (componentResult == null) {
                return null;
            }
            return toTagClassifiedResult(id, componentResult.values());
        };
    }

    /**
     * Shared first two fallback tiers of {@link #buildLookup}: an already-classified result
     * (from {@code classifiedLookup}), else a direct {@code valueTagScoresProvider} query for
     * the ingredient. Callers without access to component-classification stages (e.g.
     * {@link #mergeTagAndRecipeScores}) use this alone; {@link #buildLookup} layers its
     * {@link ComponentClassifier} tier on top.
     */
    private static Function<ResourceLocation, ClassificationResult> tagScoreFallbackLookup(
            Function<ResourceLocation, ClassificationResult> classifiedLookup,
            Function<Item, Map<String, Float>> valueTagScoresProvider
    ) {
        return id -> {
            ClassificationResult cached = classifiedLookup.apply(id);
            if (cached != null) {
                return cached;
            }
            Item ingredient = BuiltInRegistries.ITEM.get(id);
            if (ingredient == null) {
                return null;
            }
            Map<String, Float> tagScores = valueTagScoresProvider.apply(ingredient);
            if (tagScores.isEmpty()) {
                return null;
            }
            return toTagClassifiedResult(id, tagScores);
        };
    }

    private static ClassificationResult toTagClassifiedResult(ResourceLocation itemId, Map<String, Float> scores) {
        List<Map.Entry<String, Float>> sorted = scores.entrySet().stream()
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .toList();

        String dominant = sorted.get(0).getKey();
        String secondary = sorted.size() > 1 ? sorted.get(1).getKey() : null;
        float topScore = sorted.get(0).getValue();
        float secondScore = sorted.size() > 1 ? sorted.get(1).getValue() : 0f;
        float spread = topScore - secondScore;

        return new ClassificationResult(
                itemId,
                scores,
                dominant,
                secondary,
                spread,
                List.of(),
                false,
                true
        );
    }
}
