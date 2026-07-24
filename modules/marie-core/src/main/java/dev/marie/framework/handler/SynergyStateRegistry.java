package dev.marie.framework.handler;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.effects.SynergyDefinition;
import dev.marie.framework.api.value.ValueDefinition;

/**
 * Per-player synergy cooldown and active-state tracking for {@link SourceApplicationPipeline},
 * extracted to keep the pipeline's synergy loops free of raw map bookkeeping. Owned exclusively
 * by {@link SourceApplicationPipeline} via composition — not intended to be shared or held
 * elsewhere.
 */
@ApiStatus.Internal
final class SynergyStateRegistry {

    // per-player, per-synergy-id last-fired game tick — transient, not persisted
    private static final ConcurrentHashMap<UUID, Map<String, Long>> SYNERGY_LAST_FIRED =
            new ConcurrentHashMap<>();

    // tracks which value-synergy ids are currently in their "both conditions met" state per player
    private static final ConcurrentHashMap<UUID, Set<String>> VALUE_SYNERGY_ACTIVE_STATE =
            new ConcurrentHashMap<>();

    private SynergyStateRegistry() {}

    static void clearPlayer(UUID playerId) {
        SYNERGY_LAST_FIRED.remove(playerId);
        VALUE_SYNERGY_ACTIVE_STATE.remove(playerId);
    }

    static Long getLastFired(UUID playerId, String synergyId) {
        return SYNERGY_LAST_FIRED.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .get(synergyId);
    }

    static void recordFired(UUID playerId, String synergyId, long gameTick) {
        SYNERGY_LAST_FIRED.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(synergyId, gameTick);
    }

    static boolean isActive(UUID playerId, String synergyId) {
        return VALUE_SYNERGY_ACTIVE_STATE.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .contains(synergyId);
    }

    static void setActive(UUID playerId, String synergyId) {
        VALUE_SYNERGY_ACTIVE_STATE.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(synergyId);
    }

    static void clearActive(UUID playerId, String synergyId) {
        VALUE_SYNERGY_ACTIVE_STATE.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .remove(synergyId);
    }

    static boolean meetsSynergyCondition(float value, ValueDefinition def, SynergyDefinition.LevelCondition condition) {
        return switch (condition) {
            case HIGH -> value >= def.getExcessThreshold();
            case LOW -> value <= def.getLowThreshold();
            case OPTIMAL -> value > def.getLowThreshold() && value < def.getExcessThreshold();
        };
    }
}
