package dev.marie.framework.api.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.source.SourcePairSynergy;
import dev.marie.framework.api.effects.SynergyDefinition;
import dev.marie.framework.registry.ListRegistry;

import java.util.List;

/**
 * Internal storage for value synergy and source synergy definitions
 * registered via the public API.
 */
public final class SynergyRegistry {

    private static final ListRegistry<SynergyDefinition> VALUE_SYNERGIES =
            new ListRegistry<>("SynergyRegistry.valueSynergies", null);
    private static final ListRegistry<SourcePairSynergy> SOURCE_PAIR_SYNERGIES =
            new ListRegistry<>("SynergyRegistry.sourcePairSynergies", null);

    private SynergyRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        VALUE_SYNERGIES.freeze();
        SOURCE_PAIR_SYNERGIES.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        VALUE_SYNERGIES.reset();
        SOURCE_PAIR_SYNERGIES.reset();
    }

    /**
     * Registers a value synergy definition.
     *
     * @param definition the value synergy to register
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void registerValueSynergy(SynergyDefinition definition) {
        VALUE_SYNERGIES.register(definition);
    }

    /**
     * Registers a source synergy definition.
     *
     * @param definition the source synergy to register
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void registerSourcePairSynergy(SourcePairSynergy definition) {
        SOURCE_PAIR_SYNERGIES.register(definition);
    }

    /**
     * Returns all registered value synergies.
     *
     * @return an unmodifiable list of value synergy definitions
     */
    public static List<SynergyDefinition> getValueSynergies() {
        return VALUE_SYNERGIES.values();
    }

    /**
     * Returns all registered source synergies.
     *
     * @return an unmodifiable list of source synergy definitions
     */
    public static List<SourcePairSynergy> getSourcePairSynergies() {
        return SOURCE_PAIR_SYNERGIES.values();
    }
}
