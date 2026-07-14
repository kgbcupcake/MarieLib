package dev.marie.framework.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.core.KubeIntegration;
import dev.marie.framework.tracking.DiminishingReturnsConfig;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.TrackingResetSupport;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@ApiStatus.Internal
public class PlayerTrackingLifecycle {

    @SubscribeEvent
    public void onPlayerJoin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TrackingAttachment.isRegistered()) return;
        TrackingData tracking = TrackingAttachment.getData(player);
        tracking.tick();
        TrackingAttachment.setData(player, tracking);
        tracking.setMemoryConfig(DiminishingReturnsSupport.resolveMemoryConfig());
        if (MarieContext.isRegistered()) {
            MarieContext.get().syncOnJoin().accept(player);
            KubeIntegration.firePlayerSynced(player);
            if (FeatureFlagCache.enableEffects()) {
                MarieContext.get().effectApplier().accept(player, tracking);
            }
            if (MarieContext.get().showJoinMessage()) {
                player.sendSystemMessage(MarieContext.get().joinMessageLine1());
                player.sendSystemMessage(MarieContext.get().joinMessageLine2());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TrackingAttachment.isRegistered()) return;
        TrackingData tracking = TrackingAttachment.getData(player);
        tracking.tick();
        TrackingAttachment.setData(player, tracking);
        tracking.setMemoryConfig(DiminishingReturnsSupport.resolveMemoryConfig());
        TrackingResetSupport.applyRespawnValueBehavior(player, tracking);
        if (MarieContext.isRegistered()) {
            MarieContext.get().syncOnJoin().accept(player);
            KubeIntegration.firePlayerSynced(player);
            if (FeatureFlagCache.enableEffects()) {
                MarieContext.get().effectApplier().accept(player, tracking);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        DiminishingReturnsSupport.resetMemoryConfigWarning();
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TrackingAttachment.isRegistered()) return;
        TrackingData tracking = TrackingAttachment.getData(player);
        if (MarieContext.isRegistered()) {
            MarieContext.get().trackingDeltaSyncer().accept(player, tracking);
            KubeIntegration.firePlayerSynced(player);
            if (FeatureFlagCache.enableEffects()) {
                MarieContext.get().effectApplier().accept(player, tracking);
            }
        }
    }
}
