package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.source.SourcePropertySignal;
import dev.marie.framework.api.registry.SourcePropertySignalRegistry;
import dev.marie.framework.core.MarieCore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.item.ItemStack;

/**
 * Standalone signal-analysis functions used by {@link ItemClassifier#classify}, extracted to keep
 * classification orchestration and individual signal math independently readable. Each signal
 * takes only the inputs it needs rather than a shared context object, since the signals have no
 * uniform shape (unlike the {@code ResolutionStageHandler}-based stages).
 */
@ApiStatus.Internal
final class ItemClassificationSignals {

    private ItemClassificationSignals() {}

    static Map<String, Float> analyzeNamespace(String namespace, Map<String, Map<String, Float>> namespaceWeights) {
        Map<String, Float> weights = namespaceWeights.get(namespace);
        return weights != null ? new HashMap<>(weights) : Map.of();
    }

    static Map<String, Float> analyzeNegativeKeywords(String path, Map<String, Map<String, Float>> negativeKeywords) {
        Map<String, Float> contributions = new HashMap<>();

        for (String root : TokenStemmer.tokenizeForScoring(path)) {
            Map<String, Float> weights = negativeKeywords.get(root);
            if (weights != null) {
                for (Entry<String, Float> e : weights.entrySet()) {
                    contributions.merge(e.getKey(), e.getValue(), Float::sum);
                }
            }
        }

        return contributions;
    }

    static Map<String, Float> analyzeArchetypes(String path, List<ArchetypePattern> archetypes) {
        Map<String, Float> contributions = new HashMap<>();
        String lowerPath = path.toLowerCase();

        for (ArchetypePattern archetype : archetypes) {
            if (archetype.matches(lowerPath)) {
                for (Entry<String, Float> e : archetype.contributions().entrySet()) {
                    contributions.merge(e.getKey(), e.getValue(), Float::sum);
                }
            }
        }

        return contributions;
    }

    static Map<String, Float> analyzeSourcePropertySignals(ItemStack stack) {
        List<SourcePropertySignal> signals = SourcePropertySignalRegistry.getAll();
        if (signals.isEmpty()) {
            return Map.of();
        }
        Map<String, Float> contributions = new HashMap<>();
        for (SourcePropertySignal signal : signals) {
            try {
                Map<String, Float> result = signal.evaluate(stack);
                if (result != null) {
                    result.forEach((k, v) -> contributions.merge(k, v, Float::sum));
                }
            } catch (Exception ex) {
                MarieCore.LOGGER.warn(
                        "[MarieLib] SourcePropertySignal '{}' threw during evaluate(): {}",
                        signal.signalId(), ex.getMessage());
            }
        }
        return contributions;
    }

    static Map<String, Float> analyzeNamespacePeers(Map<String, Float> peerAverages, float averageWeight) {
        Map<String, Float> contributions = new HashMap<>();

        for (Entry<String, Float> e : peerAverages.entrySet()) {
            contributions.put(e.getKey(), e.getValue() * averageWeight);
        }

        return contributions;
    }
}
