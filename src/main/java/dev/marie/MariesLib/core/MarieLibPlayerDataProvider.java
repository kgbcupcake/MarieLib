package dev.marie.MariesLib.core;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MemoryView;
import net.minecraft.world.entity.player.Player;

/**
 * Runtime delegate for player data queries.
 * Registered by the consuming mod at bootstrap.
 */
@ApiStatus.Stable
public interface MarieLibPlayerDataProvider {

    float getTotal(Player player);

    float getValueLevel(Player player, String valueKey);

    MemoryView getSourceMemoryView(Player player);

    void modifyValue(Player player, String valueKey, float delta);
}
