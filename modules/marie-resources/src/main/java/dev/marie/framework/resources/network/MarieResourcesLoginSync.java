package dev.marie.framework.resources.network;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * On every player login, sends each registered config-sync registry's freshly-built snapshot to
 * the joining player. Kept separate from {@link MarieResourcesNetworking} because this hooks the
 * game event bus while payload registration hooks the mod bus.
 */
@ApiStatus.Internal
@EventBusSubscriber(modid = MarieCore.MOD_ID)
public final class MarieResourcesLoginSync {

    private MarieResourcesLoginSync() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MarieResourcesNetworking.sendAllTo(player);
    }
}
