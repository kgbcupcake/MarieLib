package dev.marie.framework.core;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.reporting.ApplicationHistoryView;
import net.minecraft.world.entity.player.Player;

/**
 * Runtime delegate for player data queries.
 * Registered by the consuming mod at bootstrap.
 */
@ApiStatus.Stable
public interface MarieDataProvider {

    float getAggregateLevel(Player player);

    float getValueLevel(Player player, String valueKey);

    ApplicationHistoryView getApplicationHistoryView(Player player);

    void modifyValue(Player player, String valueKey, float delta);
}
