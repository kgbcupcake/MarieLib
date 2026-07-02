package dev.marie.framework.classification;

/**
 * Identifies each decision point in the classification pipeline.
 */
public enum TraceStepId {
    ITEM_DISCOVERY,
    VALUE_TAG_LOOKUP,
    EXTERNAL_CLASSIFICATION,
    RESOLVER_CACHE,
    COMMUNITY_TAG_SIGNAL,
    KEYWORD_SUFFIX_SCORING,
    RECIPE_LOOKUP,
    INGREDIENT_RESOLUTION,
    NAMESPACE_PEER,
    PRIMARY_RECIPE_MERGE,
    TAG_RUNTIME_BLEND,
    SIGNAL_AGGREGATION,
    WINNER_SELECTION,
    CONFIDENCE,
    HARD_FALLBACK,
    APPLY_GATE
}
