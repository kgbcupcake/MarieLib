package dev.marie.MariesLib.core;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * Library-owned scanner and debug settings. Gameplay configuration
 * lives on the consuming mod's {@link MarieLibContext}.
 */
@ApiStatus.Stable
public interface MarieLibSettings {

    static MarieLibSettings get() {
        if (MarieLibContext.isRegistered()) {
            return MarieLibContext.get();
        }
        return MariesLibInternalContext.get();
    }

    String modId();

    float scannerConfidenceSpreadThreshold();

    float compositeRatioThreshold();

    boolean scannerEnableRecipeInheritance();

    boolean enableDebugLogging();

    double multiValueInheritanceThreshold();
}
