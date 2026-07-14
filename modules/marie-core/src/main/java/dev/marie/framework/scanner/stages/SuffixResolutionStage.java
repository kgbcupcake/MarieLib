package dev.marie.framework.scanner.stages;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scan.RuntimeCascadeStage;
import dev.marie.framework.scan.StageContext;
import dev.marie.framework.scanner.ScannerSpecRegistry;
import dev.marie.framework.scanner.ScannerSpecRegistry.Multipliers;
import dev.marie.framework.scanner.ScannerSpecRegistry.ScannerSpec;
import dev.marie.framework.scanner.TokenStemmer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Scores the trailing (and secondary trailing) path token against the configured suffix
 * weight table. Mechanical extraction of the scanner's former {@code analyzeSignal3Suffix} logic
 * into the {@link ResolutionStageHandler} cascade shape.
 */
@ApiStatus.Internal
public final class SuffixResolutionStage implements ResolutionStageHandler {

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        ScannerSpec spec = ScannerSpecRegistry.get();
        Multipliers mult = spec.multipliers();
        Map<String, Map<String, Float>> suffixWeights = spec.suffixWeights();
        String path = ctx.itemId().getPath();

        List<String> raw = TokenStemmer.rawSegmentsForPath(path);
        String[] unders = path.split("_");
        if (raw.isEmpty() && unders.length == 0) {
            return null;
        }

        Map<String, Float> contributions = new HashMap<>();
        String lastToken = !raw.isEmpty() ? raw.get(raw.size() - 1).toLowerCase(Locale.ROOT) : unders[unders.length - 1].toLowerCase(Locale.ROOT);
        Map<String, Float> weights = suffixWeights.get(lastToken);
        if (weights != null) {
            contributions.putAll(weights);
        }

        String secondLast = null;
        if (raw.size() > 1) {
            secondLast = raw.get(raw.size() - 2).toLowerCase(Locale.ROOT);
        } else if (unders.length > 1) {
            secondLast = unders[unders.length - 2].toLowerCase(Locale.ROOT);
        }

        if (secondLast != null) {
            Map<String, Float> secondWeights = suffixWeights.get(secondLast);
            if (secondWeights != null) {
                for (Entry<String, Float> e : secondWeights.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue() * mult.secondarySuffix(), Float::sum);
                }
            }
        }

        if (contributions.isEmpty()) {
            return null;
        }

        return new ResolutionResult(
                contributions, contributions, List.of(lastToken), Map.of(), Map.of(),
                false, 0f, RuntimeCascadeStage.KEYWORD_SUFFIX, "suffix_match"
        );
    }
}
