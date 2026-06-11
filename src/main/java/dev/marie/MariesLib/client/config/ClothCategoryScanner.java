package dev.marie.MariesLib.client.config;

import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigKeys;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

final class ClothCategoryScanner {

    private ClothCategoryScanner() {}

    static void build(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        MariesLibConfigHolder h = ClothConfigHelper.holder();
        var cat = ClothConfigHelper.category(builder, "scanner");

        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.SCANNER_CONFIDENCE_SPREAD_THRESHOLD,
                "scanner.confidenceSpreadThreshold", h.scannerConfidenceSpreadThreshold, 0f, 0f, 100f,
                v -> h.scannerConfidenceSpreadThreshold = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.COMPOSITE_RATIO_THRESHOLD,
                "scanner.compositeRatioThreshold", h.compositeRatioThreshold, 0f, 0f, 1f,
                v -> h.compositeRatioThreshold = v);
        ClothConfigHelper.addBool(cat, entryBuilder, MariesLibConfigKeys.SCANNER_ENABLE_RECIPE_INHERITANCE,
                "scanner.enableRecipeInheritance", h.scannerEnableRecipeInheritance, false,
                v -> h.scannerEnableRecipeInheritance = v);
        ClothConfigHelper.addDouble(cat, entryBuilder, MariesLibConfigKeys.MULTI_VALUE_INHERITANCE_THRESHOLD,
                "scanner.multiValueInheritanceThreshold", h.multiValueInheritanceThreshold, 0.20, 0.0, 1.0,
                v -> h.multiValueInheritanceThreshold = v);

        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.MULT_COMMUNITY_TAG,
                "scanner.multipliers.communityTag", h.multCommunityTag, 5f, 0f, 100f, v -> h.multCommunityTag = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.MULT_NAMESPACE,
                "scanner.multipliers.namespace", h.multNamespace, 4f, 0f, 100f, v -> h.multNamespace = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.MULT_SUFFIX,
                "scanner.multipliers.suffix", h.multSuffix, 3f, 0f, 100f, v -> h.multSuffix = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.MULT_KEYWORD,
                "scanner.multipliers.keyword", h.multKeyword, 2f, 0f, 100f, v -> h.multKeyword = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.MULT_ARCHETYPE,
                "scanner.multipliers.archetype", h.multArchetype, 2f, 0f, 100f, v -> h.multArchetype = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.MULT_RECIPE_INHERITANCE,
                "scanner.multipliers.recipeInheritance", h.multRecipeInheritance, 1f, 0f, 100f, v -> h.multRecipeInheritance = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.MULT_NAMESPACE_PEER,
                "scanner.multipliers.namespacePeer", h.multNamespacePeer, 0.5f, 0f, 100f, v -> h.multNamespacePeer = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.MULT_SECONDARY_SUFFIX,
                "scanner.multipliers.secondarySuffix", h.multSecondarySuffix, 0.5f, 0f, 100f, v -> h.multSecondarySuffix = v);
        ClothConfigHelper.addFloat(cat, entryBuilder, MariesLibConfigKeys.MULT_NAMESPACE_PEER_AVG,
                "scanner.multipliers.namespacePeerAverageWeight", h.multNamespacePeerAverageWeight, 0.5f, 0f, 100f,
                v -> h.multNamespacePeerAverageWeight = v);
    }
}
