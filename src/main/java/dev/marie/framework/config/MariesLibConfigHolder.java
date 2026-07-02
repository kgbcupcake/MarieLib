package dev.marie.framework.config;

/**
 * Mutable source of truth for all MariesLib-owned scalar configuration.
 * Only scanner and debug settings live here — gameplay config belongs to consuming mods.
 */
public final class MariesLibConfigHolder {

    private static final MariesLibConfigHolder INSTANCE = new MariesLibConfigHolder();

    // Debug
    public boolean enableDebugLogging = false;

    // Scanner context
    public float scannerConfidenceSpreadThreshold = 0f;
    public float compositeRatioThreshold = 0f;
    public boolean scannerEnableRecipeInheritance = false;
    public double multiValueInheritanceThreshold = 0.20;

    // Scanner spec multipliers
    public float multCommunityTag = 5.0f;
    public float multNamespace = 4.0f;
    public float multSuffix = 3.0f;
    public float multKeyword = 2.0f;
    public float multArchetype = 2.0f;
    public float multRecipeInheritance = 1.0f;
    public float multNamespacePeer = 0.5f;
    public float multSecondarySuffix = 0.5f;
    public float multNamespacePeerAverageWeight = 0.5f;

    private MariesLibConfigHolder() {}

    public static MariesLibConfigHolder get() {
        return INSTANCE;
    }

    /** Copies current {@link ScannerSpecRegistry} scalar values into this holder. */
    public void loadScannerScalarsFromRegistry() {
        var spec = dev.marie.framework.scanner.ScannerSpecRegistry.get();
        var mult = spec.multipliers();
        multCommunityTag = mult.communityTag();
        multNamespace = mult.namespace();
        multSuffix = mult.suffix();
        multKeyword = mult.keyword();
        multArchetype = mult.archetype();
        multRecipeInheritance = mult.recipeInheritance();
        multNamespacePeer = mult.namespacePeer();
        multSecondarySuffix = mult.secondarySuffix();
        multNamespacePeerAverageWeight = mult.namespacePeerAverageWeight();
    }
}
