package dev.marie.framework.scan;

import dev.marie.framework.api.ApiStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Merges keyword/composite primary resolution with supplementary recipe inheritance.
 *
 * <p><b>Append-only (default) behaviour:</b> recipe-derived value categories are add-only —
 * a category already present on the primary (keyword/suffix/composite) result keeps the
 * primary's weight and the keyword-matched category stays the winner. Recipe data can
 * introduce new categories but never displaces an existing one.</p>
 *
 * <p><b>Contestable categories:</b> a consuming mod may opt specific categories into
 * contestability via {@code contestable_values} in its {@code scanner_spec.json}
 * ({@link dev.marie.framework.scanner.ScannerSpecRegistry.ScannerSpec#contestableValues()}).
 * When the highest-weight <i>other</i> category in the combined (keyword-raw + decayed
 * recipe-raw) signal outweighs the keyword-matched category and that other category is
 * contestable, it becomes the winning classification outright rather than an appended
 * secondary. The opt-in list is empty by default, so this override never fires unless a mod
 * explicitly enables it; every category not on the list keeps the append-only behaviour
 * exactly.</p>
 *
 * <p><b>Confidence:</b> whenever both a primary and a recipe supplement are present, the
 * merged result's confidence is recomputed over the combined signal (spread of the top two
 * categories relative to the top weight — {@link StageMath#confidenceRatio}), never inherited
 * from {@code primary.confidence()} verbatim. Recipe data that corroborates the keyword keeps
 * confidence high; recipe data that disagrees lowers it and can push the result below
 * {@code scannerConfidenceSpreadThreshold} so it is flagged UNCERTAIN — including when the
 * keyword still won because the contested category was not opted in (recorded in the debug
 * reason for diagnostics).</p>
 */
@ApiStatus.Internal
public final class RuntimeResolutionMerge {

    private RuntimeResolutionMerge() {}

    /**
     * Combines a keyword/composite primary result with recipe inheritance output.
     *
     * @param primary           keyword/suffix/composite primary result, or {@code null}
     * @param recipe            recipe-inheritance supplement, or {@code null}
     * @param contestableValues value categories the consuming mod opted into recipe
     *                          contestability; {@code null} is treated as empty (append-only)
     * @return merged result, primary-only, recipe-only, or {@code null} when both inputs are null
     */
    @Nullable
    public static ResolutionResult mergePrimaryWithRecipeSupplement(
            @Nullable ResolutionResult primary,
            @Nullable ResolutionResult recipe,
            @Nullable Set<String> contestableValues
    ) {
        if (primary == null) {
            return recipe;
        }
        if (recipe == null) {
            return primary;
        }
        Set<String> contestable = contestableValues != null ? contestableValues : Set.of();

        // Raw, pre-collapse signal on both sides. A stage that does not populate rawScores
        // separately (leaves it empty) has its bar map used instead — same fallback shape as
        // RuntimeResolver.buildTraceFromResult. This is what lets the merge still see the full
        // keyword distribution even when the primary's values() was collapsed to {winner: 1.0}.
        Map<String, Float> primaryRaw = primary.rawScores().isEmpty() ? primary.values() : primary.rawScores();
        Map<String, Float> recipeRaw = recipe.rawScores().isEmpty() ? recipe.values() : recipe.rawScores();

        // Combined keyword-raw + decayed recipe-raw signal: drives both the contest decision
        // and the honest confidence recompute.
        Map<String, Float> combined = new LinkedHashMap<>(primaryRaw);
        for (Map.Entry<String, Float> e : recipeRaw.entrySet()) {
            combined.merge(e.getKey(), e.getValue(), Float::sum);
        }

        String keywordWinner = argmax(primaryRaw);
        float keywordWeight = keywordWinner != null ? combined.getOrDefault(keywordWinner, 0f) : 0f;

        // Highest-weight category other than the keyword winner, in the combined signal.
        String topChallenger = null;
        float topChallengerWeight = keywordWeight;
        for (Map.Entry<String, Float> e : combined.entrySet()) {
            if (e.getKey().equals(keywordWinner)) {
                continue;
            }
            if (e.getValue() > topChallengerWeight) {
                topChallenger = e.getKey();
                topChallengerWeight = e.getValue();
            }
        }

        float confidence = StageMath.confidenceRatio(combined);
        boolean overrideFires = keywordWinner != null
                && topChallenger != null
                && contestable.contains(topChallenger);

        if (overrideFires) {
            // Contestable recipe-derived category outranks the keyword match -> it wins outright.
            Map<String, Float> values = new LinkedHashMap<>();
            values.put(topChallenger, topChallengerWeight);
            for (Map.Entry<String, Float> e : combined.entrySet()) {
                if (e.getKey().equals(topChallenger)) {
                    continue;
                }
                // Clamp every other category at or below the new winner so it stays the argmax
                // even if a non-contestable category also outweighed the old keyword winner.
                values.put(e.getKey(), Math.min(e.getValue(), topChallengerWeight));
            }

            Map<String, String> rejected = new LinkedHashMap<>(primary.rejectedSignals());
            rejected.remove(topChallenger);

            String debugReason = primary.debugReason()
                    + "; recipe override: '" + topChallenger + "' (" + fmt(topChallengerWeight)
                    + ") outranked keyword '" + keywordWinner + "' (" + fmt(keywordWeight) + ") [contestable]";

            return new ResolutionResult(
                    values,
                    combined,
                    primary.tokens(),
                    primary.tokenWeights(),
                    rejected,
                    primary.cacheHit(),
                    confidence,
                    compoundStage(primary.stage()),
                    debugReason
            );
        }

        // Append-only path: keyword keeps the winning category; recipe may only add new ones.
        Map<String, Float> merged = new LinkedHashMap<>(primary.values());
        List<String> addedKeys = new ArrayList<>();
        for (Map.Entry<String, Float> e : recipe.values().entrySet()) {
            if (!merged.containsKey(e.getKey())) {
                merged.put(e.getKey(), e.getValue());
                addedKeys.add(e.getKey());
            }
        }

        Map<String, String> rejected = new LinkedHashMap<>(primary.rejectedSignals());
        for (String key : addedKeys) {
            rejected.remove(key);
        }

        RuntimeCascadeStage stage = addedKeys.isEmpty() ? primary.stage() : compoundStage(primary.stage());

        StringBuilder debugReason = new StringBuilder(primary.debugReason());
        if (!addedKeys.isEmpty()) {
            debugReason.append("; recipe supplement added [").append(String.join(",", addedKeys)).append(']');
        }
        if (keywordWinner != null && topChallenger != null) {
            // A category outweighed the keyword winner in the combined signal but held its
            // ground because the consumer did not mark it contestable — diagnostic only, the
            // winner is unchanged, but the lowered confidence above reflects the contest.
            debugReason.append("; recipe contested '").append(topChallenger).append("' (")
                    .append(fmt(topChallengerWeight)).append(") vs keyword '").append(keywordWinner)
                    .append("' (").append(fmt(keywordWeight)).append(") — keyword held (not contestable)");
        }

        return new ResolutionResult(
                merged,
                combined,
                primary.tokens(),
                primary.tokenWeights(),
                rejected,
                primary.cacheHit(),
                confidence,
                stage,
                debugReason.toString()
        );
    }

    @Nullable
    private static String argmax(Map<String, Float> scores) {
        String best = null;
        float bestVal = Float.NEGATIVE_INFINITY;
        for (Map.Entry<String, Float> e : scores.entrySet()) {
            if (e.getValue() > bestVal) {
                bestVal = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static RuntimeCascadeStage compoundStage(RuntimeCascadeStage primaryStage) {
        return switch (primaryStage) {
            case COMPOSITE -> RuntimeCascadeStage.COMPOSITE_RECIPE;
            case KEYWORD_SUFFIX -> RuntimeCascadeStage.KEYWORD_SUFFIX_RECIPE;
            default -> primaryStage;
        };
    }
}
