package dev.marie.MariesLib.api.registry;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.AbsorptionModifier;
import dev.marie.MariesLib.registry.ListRegistry;

import java.util.Comparator;
import java.util.List;

/**
 * Internal storage for value absorption modifiers registered via the public API.
 */
@ApiStatus.Internal
public final class AbsorptionModifierRegistry {

    private static final ListRegistry<AbsorptionModifier> REGISTRY = new ListRegistry<>(
            "AbsorptionModifierRegistry",
            Comparator.comparingInt(AbsorptionModifier::getPriority)
    );

    private AbsorptionModifierRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    /**
     * Registers an absorption modifier. Priority ordering is applied when the registry is frozen.
     *
     * @param modifier the absorption modifier to register
     * @throws IllegalArgumentException if {@code modifier} is null
     */
    public static void register(AbsorptionModifier modifier) {
        REGISTRY.register(modifier);
    }

    /**
     * Returns all registered absorption modifiers sorted by priority.
     *
     * @return an unmodifiable list of absorption modifiers
     */
    public static List<AbsorptionModifier> getAll() {
        return REGISTRY.values();
    }
}
