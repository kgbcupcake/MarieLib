package dev.marie.framework.kubejs.internal;


import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ValueModifierContext;
import dev.marie.framework.kubejs.MarieKubeEvents;
import dev.marie.framework.kubejs.events.MarieDecayTickEvent;
import dev.marie.framework.kubejs.events.MariePlayerSyncedEvent;
import dev.marie.framework.kubejs.events.MarieValueDeltaModifierEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Direct pipeline hooks into KubeJS events (no NeoForge intermediate).
 */
@ApiStatus.Internal
public final class KubePipelineHooks {

    private KubePipelineHooks() {}

    public static float applyValueDeltaModifier(ValueModifierContext ctx, float delta) {
        if (!KubeGuard.hasListeners(MarieKubeEvents.VALUE_DELTA_MODIFIER_ID)) {
            return delta;
        }
        MarieValueDeltaModifierEvent kube = new MarieValueDeltaModifierEvent(ctx, delta);
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
