package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@ApiStatus.Internal
public class ValueEffectsHandler {

    private static final int APPLY_INTERVAL_TICKS = 40;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % APPLY_INTERVAL_TICKS != 0) return;
        if (ReloadHandler.isReloadInProgress()) return;

        if (ModuleCache.enableEffects) {
            TrackingData data = player.getData(TrackingAttachment.TRACKING.get());
            MarieLibContext.get().effectApplier().accept(player, data);
            for (String oldId : MarieLibContext.get().previousEffectIds()) {
                if (!MarieLibContext.get().isEffectDefinitionRegistered(oldId)) {
                    BuiltInRegistries.MOB_EFFECT
                            .getHolder(ResourceLocation.parse(oldId))
                            .ifPresent(player::removeEffect);
                }
            }
        } else {
            MarieLibContext.get().effectClearer().accept(player);
        }
    }
}
