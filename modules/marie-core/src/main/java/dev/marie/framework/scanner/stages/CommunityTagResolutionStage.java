package dev.marie.framework.scanner.stages;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.scan.ResolutionResult;
import dev.marie.framework.scan.ResolutionStageHandler;
import dev.marie.framework.scan.RuntimeCascadeStage;
import dev.marie.framework.scan.StageContext;
import dev.marie.framework.scanner.InstanceTagSourceRegistry;
import dev.marie.framework.scanner.ScannerSpecRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Scores an item against the configured community tag weight table, under the {@code c}
 * namespace directory returned by {@link #tagDirectory()}. Mechanical extraction of the
 * scanner's former {@code analyzeSignal1CommunityTags} logic into the
 * {@link ResolutionStageHandler} cascade shape.
 */
@ApiStatus.Internal
public final class CommunityTagResolutionStage implements ResolutionStageHandler {

    /**
     * The {@code c} namespace tag directory this stage scans (e.g. for building display/trace
     * labels). Exposed so callers never need to hardcode a duplicate copy of this value.
     * Sourced from {@link ScannerSpecRegistry.ScannerSpec#communityTagDirectory()}, which each
     * consumer mod may override in its own {@code scanner_spec.json}.
     */
    public static String tagDirectory() {
        return ScannerSpecRegistry.get().communityTagDirectory();
    }

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        Map<String, Map<String, Float>> communityTagWeights = ScannerSpecRegistry.get().communityTagWeights();
        String tagDirectory = ScannerSpecRegistry.get().communityTagDirectory();
        Map<String, Float> contributions = new HashMap<>();
        String modId = MarieContext.isRegistered() ? MarieContext.get().modId() : null;

        for (Entry<String, Map<String, Float>> entry : communityTagWeights.entrySet()) {
            String tagSuffix = entry.getKey();
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", tagDirectory + tagSuffix));
            boolean inInstanceTags = modId != null && InstanceTagSourceRegistry.contains(modId, tagSuffix, itemId);
            if (ctx.holder().is(tagKey) || inInstanceTags) {
                for (Entry<String, Float> contrib : entry.getValue().entrySet()) {
                    contributions.merge(contrib.getKey(), contrib.getValue(), Float::sum);
                }
            }
        }

        if (contributions.isEmpty()) {
            return null;
        }

        ctx.communityTagSignal().putAll(contributions);
        return new ResolutionResult(
                contributions, contributions, List.of(), Map.of(), Map.of(),
                false, 0f, RuntimeCascadeStage.COMMUNITY_TAG, "community_tag_match"
        );
    }
}
