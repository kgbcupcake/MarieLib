package dev.marie.framework.core;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.config.MariesLibConfigHolder;

@ApiStatus.Internal
final class MariesLibInternalContext implements MarieLibSettings {

    private static final MariesLibInternalContext INSTANCE = new MariesLibInternalContext();

    private MariesLibInternalContext() {}

    static MariesLibInternalContext get() {
        return INSTANCE;
    }

    @Override
    public String modId() {
        return MariesLib.MOD_ID;
    }

    @Override
    public float scannerConfidenceSpreadThreshold() {
        return MariesLibConfigHolder.get().scannerConfidenceSpreadThreshold;
    }

    @Override
    public float compositeRatioThreshold() {
        return MariesLibConfigHolder.get().compositeRatioThreshold;
    }

    @Override
    public boolean scannerEnableRecipeInheritance() {
        return MariesLibConfigHolder.get().scannerEnableRecipeInheritance;
    }

    @Override
    public boolean enableDebugLogging() {
        return MariesLibConfigHolder.get().enableDebugLogging;
    }

    @Override
    public double multiValueInheritanceThreshold() {
        return MariesLibConfigHolder.get().multiValueInheritanceThreshold;
    }
}
