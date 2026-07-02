package dev.marie.framework.api;

import dev.marie.framework.api.ApiStatus;

import java.util.Map;

/**
 * Immutable snapshot of a player's current value state.
 *
 * <p>This record combines total totals, value levels, and source memory
 * into a single API value object suitable for read-only consumption.</p>
 *
 * @param total   the player's current total value
 * @param values  unmodifiable value map keyed by value id
 * @param sourceMemory read-only view of the player's source memory state
 */
@ApiStatus.Stable
public record MariePlayerData(float total, Map<String, Float> values, ApplicationHistoryView sourceMemory) {
}
