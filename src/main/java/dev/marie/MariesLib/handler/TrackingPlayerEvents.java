package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@ApiStatus.Internal
public class TrackingPlayerEvents {

    @SubscribeEvent
    public void onPlayerJoin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TrackingData tracking = player.getData(TrackingAttachment.TRACKING.get());
        tracking.tick();
        player.setData(TrackingAttachment.TRACKING.get(), tracking);
        tracking.setMemoryConfig(HandlerSupport.resolveMemoryConfig());
        MarieLibContext.get().syncOnJoin().accept(player);
        if (ModuleCache.enableEffects) {
            MarieLibContext.get().effectApplier().accept(player, tracking);
        }
        if (MarieLibContext.get().showJoinMessage()) {
            player.sendSystemMessage(MarieLibContext.get().joinMessageLine1());
            player.sendSystemMessage(MarieLibContext.get().joinMessageLine2());
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TrackingData tracking = player.getData(TrackingAttachment.TRACKING.get());
        tracking.tick();
        player.setData(TrackingAttachment.TRACKING.get(), tracking);
        tracking.setMemoryConfig(HandlerSupport.resolveMemoryConfig());
        MarieLibContext.get().syncOnJoin().accept(player);
        if (ModuleCache.enableEffects) {
            MarieLibContext.get().effectApplier().accept(player, tracking);
        }
    }

    @SubscribeEvent
    public void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        HandlerSupport.resetMemoryConfigWarning();
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TrackingData tracking = player.getData(TrackingAttachment.TRACKING.get());
        MarieLibContext.get().trackingDeltaSyncer().accept(player, tracking);
        if (ModuleCache.enableEffects) {
            MarieLibContext.get().effectApplier().accept(player, tracking);
        }
    }
}
