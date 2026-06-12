package dev.marie.MariesLib.kubejs.internal;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.kubejs.MarieKubeEvents;
import dev.marie.MariesLib.kubejs.events.MarieDecayTickEvent;
import dev.marie.MariesLib.kubejs.events.MariePlayerSyncedEvent;
import dev.marie.MariesLib.kubejs.events.MarieValueDeltaModifierEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Direct pipeline hooks into KubeJS events (no NeoForge intermediate).
 */
@ApiStatus.Internal
public final class KubePipelineHooks {

    private KubePipelineHooks() {}

    public static float applyValueDeltaModifier(
            String playerId,
            String itemId,
            String valueKey,
            float delta
    ) {
        if (!KubeGuard.hasListeners(MarieKubeEvents.VALUE_DELTA_MODIFIER_ID)) {
            return delta;
        }
        MarieValueDeltaModifierEvent kube =
                new MarieValueDeltaModifierEvent(playerId, itemId, valueKey, delta);
        MarieKubeEvents.VALUE_DELTA_MODIFIER.post(kube);
        return kube.getAmount();
    }

    public static float applyDecayTick(String playerId, String valueKey, float amount) {
        if (!KubeGuard.hasListeners(MarieKubeEvents.DECAY_TICK_ID)) {
            return amount;
        }
        MarieDecayTickEvent kube = new MarieDecayTickEvent(playerId, valueKey, amount);
        MarieKubeEvents.DECAY_TICK.post(kube);
        return kube.isCancelled() ? 0f : kube.getAmount();
    }

    public static void firePlayerSynced(ServerPlayer player) {
        if (!KubeGuard.hasListeners(MarieKubeEvents.PLAYER_SYNCED_ID)) {
            return;
        }
        MarieKubeEvents.PLAYER_SYNCED.post(
                new MariePlayerSyncedEvent(player.getUUID().toString()));
    }
}
