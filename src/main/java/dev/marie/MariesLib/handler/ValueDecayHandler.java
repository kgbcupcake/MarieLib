package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieEvents;
import dev.marie.MariesLib.api.MarieSeasonHook;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.registry.SeasonHookRegistry;
import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.IMarieLibConfig;
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
        int interval = Math.max(1, IMarieLibConfig.get().decayIntervalTicks());
        if (player.level().getGameTime() % interval != 0) return;
        boolean changed = false;
        for (ValueDefinition def : ValueRegistry.getAll()) {
            String key = def.getId();
            float rate = def.getDefaultDecayRate();
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

                    if (MarieLibContext.isValueBeneficial(key)) {
                        float criticalThreshold = IMarieLibConfig.get().criticalThresholdFor(key);
                        if (newValue <= criticalThreshold && current > criticalThreshold) {
                            NeoForge.EVENT_BUS.post(new MarieEvents.ValueCriticalEvent(player, key));
                        }
                    }
                }
            }
        }

        if (changed) {
            TrackingAttachment.setData(player, data);
            if (MarieLibContext.isRegistered()) {
                MarieLibContext.get().trackingDeltaSyncer().accept(player, data);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        SNAPSHOT_WARN_ONCE.set(false);
        SourceApplicationPipeline.resetSnapshotWarnings();
        HandlerSupport.resetMemoryConfigWarning();
    }

    private static TrackingMemoryConfigOrSkip resolveConfigOrSkip() {
        return new TrackingMemoryConfigOrSkip(IMarieLibConfig.get().trackingMemoryConfig(), false);
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
