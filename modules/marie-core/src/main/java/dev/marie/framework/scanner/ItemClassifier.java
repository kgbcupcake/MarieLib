package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.SourcePropertySignal;
import dev.marie.framework.api.registry.SourcePropertySignalRegistry;
import dev.marie.framework.classification.ClassificationTraceStep;
import dev.marie.framework.classification.TraceStepId;
import dev.marie.framework.classification.TraceStepStatus;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.scanner.ScannerSpecRegistry.Multipliers;
import dev.marie.framework.scanner.ScannerSpecRegistry.ScannerSpec;
import dev.marie.framework.scanner.stages.RecipeInheritanceStage;
import dev.marie.framework.util.MarieRegistryUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@ApiStatus.Internal
public final class ItemClassifier {
   /** NeoForge community source-capable tag directory under namespace {@code c}. */
   private static final String C_COMMUNITY_TAG_PREFIX = "fo" + "ods/";

   private final RecipeInheritanceResolver recipeResolver;
   private final boolean enableRecipeInheritance;
   private final float confidenceSpreadThreshold;
   private final List<String> valueKeys;

   public ItemClassifier(
      List<String> valueKeys, boolean enableRecipeInheritance, float confidenceSpreadThreshold, @Nullable RecipeInheritanceResolver recipeResolver
   ) {
      this.valueKeys = List.copyOf(valueKeys);
      this.enableRecipeInheritance = enableRecipeInheritance;
      this.confidenceSpreadThreshold = confidenceSpreadThreshold;
      this.recipeResolver = recipeResolver;
   }

   public ClassificationResult classify(
      Item item, Function<ResourceLocation, ClassificationResult> classifiedLookup, Map<String, Map<String, Float>> namespaceAverages
   ) {
      return this.classify(item, classifiedLookup, namespaceAverages, Optional.empty());
   }

   public ClassificationResult classify(
      Item item,
      Function<ResourceLocation, ClassificationResult> classifiedLookup,
      Map<String, Map<String, Float>> namespaceAverages,
      Optional<List<ClassificationTraceStep>> traceOut
   ) {
      ResourceLocation itemId = MarieRegistryUtils.itemKey(item);
      if (itemId == null) {
         return ClassificationResult.empty(ResourceLocation.withDefaultNamespace("unknown"), this.valueKeys.get(0));
      } else {
         ScannerSpec spec = ScannerSpecRegistry.get();
         Multipliers mult = spec.multipliers();
         ItemStack stack = new ItemStack(item);
         Holder<Item> holder = stack.getItemHolder();
         String namespace = itemId.getNamespace();
         String path = itemId.getPath();
         boolean hasSourceProperties = !MarieContext.isRegistered()
                 || MarieContext.get().sourceItemFilter().test(stack);
         if (traceOut.isPresent()) {
            Map<String, Object> discoveryDetail = new LinkedHashMap<>();
            discoveryDetail.put("itemId", itemId.toString());
            discoveryDetail.put("passesSourceFilter", hasSourceProperties);
            discoveryDetail.put("hasTag", false);
            if (!hasSourceProperties) {
               discoveryDetail.put("errorCode", "NOT_SOURCE_CAPABLE");
            }

            traceOut.get()
               .add(
                  new ClassificationTraceStep(
                     TraceStepId.ITEM_DISCOVERY,
                     hasSourceProperties ? TraceStepStatus.SUCCESS : TraceStepStatus.FAILURE,
                     hasSourceProperties ? "Source-capable item discovered" : "Item failed source filter",
                     discoveryDetail
                  )
               );
         }

         Map<String, Float> scores = new HashMap<>();

         for (String key : this.valueKeys) {
            scores.put(key, 0.0F);
         }

         List<ClassificationSignal> signals = new ArrayList<>();
         Map<String, Float> communityTagContribs = this.analyzeSignal1CommunityTags(holder, spec.communityTagWeights());
         this.applySignal(scores, communityTagContribs, mult.communityTag());
         if (!communityTagContribs.isEmpty()) {
            signals.add(new ClassificationSignal("COMMUNITY_TAG", "c:" + C_COMMUNITY_TAG_PREFIX + "*", this.scaleContributions(communityTagContribs, mult.communityTag())));
         }

         this.emitSignalStep(
            traceOut,
            TraceStepId.COMMUNITY_TAG_SIGNAL,
            "community_tag",
            communityTagContribs,
            this.scaleContributions(communityTagContribs, mult.communityTag())
         );
         Map<String, Float> namespaceContribs = this.analyzeSignal2Namespace(namespace, spec.namespaceWeights());
         this.applySignal(scores, namespaceContribs, mult.namespace());
         if (!namespaceContribs.isEmpty()) {
            signals.add(new ClassificationSignal("NAMESPACE", namespace, this.scaleContributions(namespaceContribs, mult.namespace())));
         }

         Map<String, Float> suffixContribs = this.analyzeSignal3Suffix(path, spec.suffixWeights(), mult.secondarySuffix());
         this.applySignal(scores, suffixContribs, mult.suffix());
         if (!suffixContribs.isEmpty()) {
            signals.add(new ClassificationSignal("SUFFIX", this.extractTrailingToken(path), this.scaleContributions(suffixContribs, mult.suffix())));
         }

         Map<String, Float> keywordContribs = this.analyzeSignal4Keywords(path, spec.keywordWeights());
         this.applySignal(scores, keywordContribs, mult.keyword());
         if (!keywordContribs.isEmpty()) {
            signals.add(new ClassificationSignal("KEYWORD", path, this.scaleContributions(keywordContribs, mult.keyword())));
         }

         this.emitSignalStep(
            traceOut, TraceStepId.KEYWORD_SUFFIX_SCORING, "keyword_suffix", keywordContribs, this.scaleContributions(keywordContribs, mult.keyword())
         );
         Map<String, Float> negativeContribs = this.analyzeSignal5NegativeKeywords(path, spec.negativeKeywords());
         this.applySignal(scores, negativeContribs, 1.0F);
         if (!negativeContribs.isEmpty()) {
            signals.add(new ClassificationSignal("NEGATIVE_KEYWORD", path, negativeContribs));
         }

         Map<String, Float> archetypeContribs = this.analyzeSignal6Archetypes(path, spec.archetypes());
         this.applySignal(scores, archetypeContribs, mult.archetype());
         if (!archetypeContribs.isEmpty()) {
            signals.add(new ClassificationSignal("ARCHETYPE", path, this.scaleContributions(archetypeContribs, mult.archetype())));
         }

         Map<String, Float> sourcePropContribs = this.analyzeSignal7SourcePropertySignals(stack);
         this.applySignal(scores, sourcePropContribs, 1.0F);
         if (!sourcePropContribs.isEmpty()) {
            signals.add(
               new ClassificationSignal(
                  ClassificationSignal.TYPE_SOURCE_PROPERTIES,
                  "source_property_signals",
                  sourcePropContribs
               )
            );
         }

         RecipeInheritanceStage.apply(
            item,
            itemId,
            scores,
            signals,
            false,
            this.enableRecipeInheritance ? this.recipeResolver : null,
            classifiedLookup,
            mult.recipeInheritance(),
            i -> this.analyzeSignal1CommunityTags(BuiltInRegistries.ITEM.wrapAsHolder(i), spec.communityTagWeights())
         );
         Map<String, Float> peerAvg = namespaceAverages.get(namespace);
         if (peerAvg != null && !peerAvg.isEmpty()) {
            Map<String, Float> peerContribs = this.analyzeSignal9NamespacePeers(peerAvg, mult.namespacePeerAverageWeight());
            this.applySignal(scores, peerContribs, mult.namespacePeer());
            if (!peerContribs.isEmpty()) {
               signals.add(new ClassificationSignal("NAMESPACE_PEER", namespace + "_peers", this.scaleContributions(peerContribs, mult.namespacePeer())));
            }

            this.emitSignalStep(
               traceOut, TraceStepId.NAMESPACE_PEER, "namespace_peer", peerContribs, this.scaleContributions(peerContribs, mult.namespacePeer())
            );
         }

         if (traceOut.isPresent()) {
            Map<String, Object> aggregationDetail = new LinkedHashMap<>();
            aggregationDetail.put("mergedScores", new LinkedHashMap<>(scores));
            aggregationDetail.put("signalCount", signals.size());
            traceOut.get()
               .add(
                  new ClassificationTraceStep(
                     TraceStepId.SIGNAL_AGGREGATION, TraceStepStatus.SUCCESS, "Aggregated " + signals.size() + " signal(s)", aggregationDetail
                  )
               );
         }

         return this.buildResult(itemId, scores, signals, traceOut);
      }
   }

   private void emitSignalStep(
      Optional<List<ClassificationTraceStep>> traceOut,
      TraceStepId stepId,
      String signalType,
      Map<String, Float> rawContribs,
      Map<String, Float> scaledContribs
   ) {
      if (!traceOut.isEmpty()) {
         Map<String, Object> detail = new LinkedHashMap<>();
         detail.put("signalType", signalType);
         detail.put("contributions", new LinkedHashMap<>(scaledContribs));
         boolean hasSignal = !rawContribs.isEmpty();
         traceOut.get()
            .add(
               new ClassificationTraceStep(
                  stepId,
                  hasSignal ? TraceStepStatus.SUCCESS : TraceStepStatus.SKIPPED,
                  hasSignal ? signalType + " signal contributed scores" : "No " + signalType + " signal match",
                  detail
               )
            );
      }
   }

   private Map<String, Float> analyzeSignal1CommunityTags(Holder<Item> holder, Map<String, Map<String, Float>> communityTagWeights) {
      Map<String, Float> contributions = new HashMap<>();

      for (Entry<String, Map<String, Float>> entry : communityTagWeights.entrySet()) {
         String tagSuffix = entry.getKey();
         TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", C_COMMUNITY_TAG_PREFIX + tagSuffix));
         if (holder.is(tagKey)) {
            for (Entry<String, Float> contrib : entry.getValue().entrySet()) {
               contributions.merge(contrib.getKey(), contrib.getValue(), Float::sum);
            }
         }
      }

      return contributions;
   }

   private Map<String, Float> analyzeSignal2Namespace(String namespace, Map<String, Map<String, Float>> namespaceWeights) {
      Map<String, Float> weights = namespaceWeights.get(namespace);
      return (Map<String, Float>)(weights != null ? new HashMap<>(weights) : Map.of());
   }

   private Map<String, Float> analyzeSignal3Suffix(String path, Map<String, Map<String, Float>> suffixWeights, float secondarySuffixWeight) {
      Map<String, Float> contributions = new HashMap<>();
      List<String> raw = TokenStemmer.rawSegmentsForPath(path);
      String[] unders = path.split("_");
      if (raw.isEmpty() && unders.length == 0) {
         return contributions;
      } else {
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
                  contributions.merge(e.getKey(), e.getValue() * secondarySuffixWeight, Float::sum);
               }
            }
         }

         return contributions;
      }
   }

   private Map<String, Float> analyzeSignal4Keywords(String path, Map<String, Map<String, Float>> keywordWeights) {
      Map<String, Float> contributions = new HashMap<>();

      for (String root : TokenStemmer.tokenizeForScoring(path)) {
         Map<String, Float> weights = keywordWeights.get(root);
         if (weights != null) {
            for (Entry<String, Float> e : weights.entrySet()) {
               contributions.merge(e.getKey(), e.getValue(), Float::sum);
            }
         }
      }

      return contributions;
   }

   private Map<String, Float> analyzeSignal5NegativeKeywords(String path, Map<String, Map<String, Float>> negativeKeywords) {
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

   private Map<String, Float> analyzeSignal6Archetypes(String path, List<ArchetypePattern> archetypes) {
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

   private Map<String, Float> analyzeSignal7SourcePropertySignals(ItemStack stack) {
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

   private Map<String, Float> analyzeSignal9NamespacePeers(Map<String, Float> peerAverages, float averageWeight) {
      Map<String, Float> contributions = new HashMap<>();

      for (Entry<String, Float> e : peerAverages.entrySet()) {
         contributions.put(e.getKey(), e.getValue() * averageWeight);
      }

      return contributions;
   }

   private void applySignal(Map<String, Float> scores, Map<String, Float> contributions, float multiplier) {
      for (Entry<String, Float> e : contributions.entrySet()) {
         String key = e.getKey();
         if (scores.containsKey(key)) {
            scores.put(key, scores.get(key) + e.getValue() * multiplier);
         }
      }
   }

   private Map<String, Float> scaleContributions(Map<String, Float> contributions, float multiplier) {
      Map<String, Float> scaled = new HashMap<>();

      for (Entry<String, Float> e : contributions.entrySet()) {
         scaled.put(e.getKey(), e.getValue() * multiplier);
      }

      return scaled;
   }

   private String extractTrailingToken(String path) {
      List<String> raw = TokenStemmer.rawSegmentsForPath(path);
      if (!raw.isEmpty()) {
         return raw.get(raw.size() - 1);
      } else {
         String[] tokens = path.split("_");
         return tokens.length > 0 ? tokens[tokens.length - 1] : path;
      }
   }

   private ClassificationResult buildResult(ResourceLocation itemId, Map<String, Float> scores, List<ClassificationSignal> signals) {
      return this.buildResult(itemId, scores, signals, Optional.empty());
   }

   private ClassificationResult buildResult(
      ResourceLocation itemId, Map<String, Float> scores, List<ClassificationSignal> signals, Optional<List<ClassificationTraceStep>> traceOut
   ) {
      List<Entry<String, Float>> sorted = scores.entrySet().stream().sorted((a, b) -> Float.compare(b.getValue(), a.getValue())).toList();
      if (sorted.isEmpty()) {
         return ClassificationResult.empty(itemId, this.valueKeys.get(0));
      } else {
         String dominant = sorted.get(0).getKey();
         String secondary = sorted.size() > 1 ? sorted.get(1).getKey() : null;
         float topScore = sorted.get(0).getValue();
         float secondScore = sorted.size() > 1 ? sorted.get(1).getValue() : 0.0F;
         float spread = topScore - secondScore;
         boolean uncertain = spread < this.confidenceSpreadThreshold;
         if (traceOut.isPresent()) {
            Map<String, Object> winnerDetail = new LinkedHashMap<>();
            winnerDetail.put("dominant", dominant);
            if (secondary != null) {
               winnerDetail.put("secondary", secondary);
            }

            winnerDetail.put("spread", (double)spread);
            traceOut.get()
               .add(
                  new ClassificationTraceStep(
                     TraceStepId.WINNER_SELECTION,
                     TraceStepStatus.SUCCESS,
                     "Selected " + dominant + " as dominant (spread=" + String.format("%.2f", spread) + ")",
                     winnerDetail
                  )
               );
            Map<String, Object> confidenceDetail = new LinkedHashMap<>();
            confidenceDetail.put("spread", (double)spread);
            confidenceDetail.put("threshold", (double)this.confidenceSpreadThreshold);
            confidenceDetail.put("uncertain", uncertain);
            float confidenceScore = topScore > 0.0F ? spread / topScore : 0.0F;
            confidenceDetail.put("confidenceScore", (double)Math.max(0.0F, Math.min(1.0F, confidenceScore)));
            if (uncertain) {
               confidenceDetail.put("warningCode", "UNCERTAIN_SPREAD");
            }

            traceOut.get()
               .add(
                  new ClassificationTraceStep(
                     TraceStepId.CONFIDENCE,
                     uncertain ? TraceStepStatus.WARNING : TraceStepStatus.SUCCESS,
                     uncertain ? "Low confidence (spread below threshold)" : "Confident classification",
                     confidenceDetail
                  )
               );
         }

         return new ClassificationResult(itemId, scores, dominant, secondary, spread, signals, uncertain, false);
      }
   }

   public static Map<String, Map<String, Float>> computeNamespaceAverages(Map<ResourceLocation, ClassificationResult> results) {
      Map<String, List<ClassificationResult>> byNamespace = new HashMap<>();

      for (Entry<ResourceLocation, ClassificationResult> e : results.entrySet()) {
         String ns = e.getKey().getNamespace();
         byNamespace.computeIfAbsent(ns, k -> new ArrayList<>()).add(e.getValue());
      }

      Map<String, Map<String, Float>> averages = new HashMap<>();

      for (Entry<String, List<ClassificationResult>> e : byNamespace.entrySet()) {
         List<ClassificationResult> list = e.getValue();
         if (!list.isEmpty()) {
            Map<String, Float> summed = new HashMap<>();

            for (ClassificationResult r : list) {
               for (Entry<String, Float> score : r.scores().entrySet()) {
                  summed.merge(score.getKey(), score.getValue(), Float::sum);
               }
            }

            Map<String, Float> avg = new HashMap<>();

            for (Entry<String, Float> s : summed.entrySet()) {
               avg.put(s.getKey(), s.getValue() / (float)list.size());
            }

            averages.put(e.getKey(), avg);
         }
      }

      return averages;
   }
}
