package dev.marie.framework.scanner.stages;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scan.RuntimeCascadeStage;
import dev.marie.framework.scan.StageContext;
import dev.marie.framework.scan.StageMath;
import dev.marie.framework.scanner.ScannerSpecRegistry;
import dev.marie.framework.scanner.TokenStemmer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Scores stemmed path tokens against the configured keyword weight table. Mechanical
 * extraction of the scanner's former {@code analyzeSignal4Keywords} logic into the
 * {@link ResolutionStageHandler} cascade shape.
 */
@ApiStatus.Internal
public final class KeywordResolutionStage implements ResolutionStageHandler {

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        Map<String, Map<String, Float>> keywordWeights = ScannerSpecRegistry.get().keywordWeights();
        String path = ctx.itemId().getPath();

        Map<String, Float> contributions = new HashMap<>();
        List<String> matchedTokens = new ArrayList<>();

        for (String root : TokenStemmer.tokenizeForScoring(path)) {
            Map<String, Float> weights = keywordWeights.get(root);
            if (weights != null) {
                matchedTokens.add(root);
                for (Entry<String, Float> e : weights.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue(), Float::sum);
                }
            }
        }

        if (contributions.isEmpty()) {
            return null;
        }

        // Honest confidence over the raw contributions (spread of the top two categories
        // relative to the top weight), not a hardcoded 0f — so downstream never has to treat
        // a genuinely contested keyword match as maximally confident. rawScores == values here
        // because this stage does no collapse; RuntimeResolutionMerge relies on rawScores
        // still carrying the full pre-collapse distribution.
        float confidence = StageMath.confidenceRatio(contributions);

        return new ResolutionResult(
                contributions, contributions, matchedTokens, Map.of(), Map.of(),
                false, confidence, RuntimeCascadeStage.KEYWORD_SUFFIX, "keyword_match"
        );
    }
}
