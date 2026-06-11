package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieEvents;
import dev.marie.MariesLib.api.MarieSeasonHook;
import dev.marie.MariesLib.api.registry.SeasonHookRegistry;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.registry.MarieAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@ApiStatus.Internal
public class ValueDecayHandler {

    private static final java.util.concurrent.atomic.AtomicBoolean SNAPSHOT_WARN_ONCE =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!ModuleCache.enableDecay) return;
        if (ReloadHandler.isReloadInProgress()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TrackingMemoryConfigOrSkip configOrSkip = resolveConfigOrSkip();
        if (!TrackingAttachment.isRegistered()) return;
        TrackingData data = TrackingAttachment.getData(player);
        if (configOrSkip.skipDecay()) {
            data.setMemoryConfig(configOrSkip.config());
            return;
        }
        data.setMemoryConfig(configOrSkip.config());
        int interval = Math.max(1, MarieLibContext.get().decayIntervalTicks());
        if (player.level().getGameTime() % interval != 0) return;
        boolean changed = false;
        for (String key : MarieLibContext.get().valueKeys()) {
            float rate = MarieLibContext.get().valueDecayRateProvider().apply(key);
            rate = applySeasonalDecayModifier(key, rate);
            rate *= MarieAttributes.valueDecayMultiplier(player);
            float current = data.values.getOrDefault(key, 0f);
            if (current > 0f) {
                float newValue = Math.max(0f, current - rate);
                data.values.put(key, newValue);
                changed = true;

                if (current != newValue) {
                    NeoForge.EVENT_BUS.post(new MarieEvents.ValueChangedEvent(
                            player, key, current, newValue));

                    if (MarieLibContext.get().isValueBeneficial().test(key)) {
                        float criticalThreshold = MarieLibContext.get().criticalThresholdFor(key);
                        if (newValue <= criticalThreshold && current > criticalThreshold) {
                            NeoForge.EVENT_BUS.post(new MarieEvents.ValueCriticalEvent(player, key));
                        }
                    }
                }
            }
        }

        if (changed) {
            TrackingAttachment.setData(player, data);
            MarieLibContext.get().trackingDeltaSyncer().accept(player, data);
        }
    }

    @SubscribeEvent
    public void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        SNAPSHOT_WARN_ONCE.set(false);
        SourceApplicationPipeline.resetSnapshotWarnings();
        HandlerSupport.resetMemoryConfigWarning();
    }

    private static TrackingMemoryConfigOrSkip resolveConfigOrSkip() {
        var provider = MarieLibContext.get().trackingMemoryConfigProvider();
        if (provider.get() != null) {
            return new TrackingMemoryConfigOrSkip(provider.get(), false);
        }
        if (SNAPSHOT_WARN_ONCE.compareAndSet(false, true)) {
            dev.marie.MariesLib.core.MariesLib.LOGGER.warn(
                    "[MarieLib] ValueDecayHandler: trackingMemoryConfigProvider returned null, decay skipped. Will not warn again until server restart.");
        }
        return new TrackingMemoryConfigOrSkip(HandlerSupport.resolveMemoryConfig(), true);
    }

    private float applySeasonalDecayModifier(String valueKey, float baseRate) {
        var hooks = SeasonHookRegistry.getAll();
        if (!ModuleCache.enableSeasonHooks || hooks.isEmpty()) {
            return baseRate;
        }
        float rate = baseRate;
        for (MarieSeasonHook hook : hooks) {
            float seasonal = Math.max(0f, hook.getSeasonalDecayModifier(valueKey, MarieSeasonHook.Season.SPRING));
            rate *= seasonal;
        }
        return rate;
    }

    private record TrackingMemoryConfigOrSkip(
            dev.marie.MariesLib.tracking.TrackingMemoryConfig config,
            boolean skipDecay
    ) {}
}
