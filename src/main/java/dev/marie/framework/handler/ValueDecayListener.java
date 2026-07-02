package dev.marie.framework.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.MarieEvents;
import dev.marie.framework.api.MarieSeasonHook;
import dev.marie.framework.api.ValueDefinition;
import dev.marie.framework.api.registry.SeasonHookRegistry;
import dev.marie.framework.api.registry.ValueRegistry;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.core.IMarieLibConfig;
import dev.marie.framework.core.MarieLibContext;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.core.KubeIntegration;
import dev.marie.framework.registry.MarieAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@ApiStatus.Internal
public class ValueDecayListener {

    private static final java.util.concurrent.atomic.AtomicBoolean SNAPSHOT_WARN_ONCE =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!FeatureFlagCache.enableDecay()) return;
        if (ReloadGuardListener.isReloadInProgress()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DiminishingReturnsConfigOrSkip configOrSkip = resolveConfigOrSkip();
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
            float rate = IMarieLibConfig.get().decayRateFor(key);
            rate = applySeasonalDecayModifier(key, rate);
            rate *= MarieAttributes.valueDecayMultiplier(player);
            float current = data.values.getOrDefault(key, 0f);
            if (current > 0f) {
                float decayAmount = KubeIntegration.applyDecayTick(
                        player.getUUID().toString(),
                        key,
                        rate);
                if (decayAmount <= 0f) {
                    continue;
                }
                float newValue = Math.max(0f, current - decayAmount);
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
        DiminishingReturnsSupport.resetMemoryConfigWarning();
    }

    private static DiminishingReturnsConfigOrSkip resolveConfigOrSkip() {
        return new DiminishingReturnsConfigOrSkip(IMarieLibConfig.get().trackingMemoryConfig(), false);
    }

    private float applySeasonalDecayModifier(String valueKey, float baseRate) {
        var hooks = SeasonHookRegistry.getAll();
        if (!FeatureFlagCache.enableSeasonHooks() || hooks.isEmpty()) {
            return baseRate;
        }
        float rate = baseRate;
        for (MarieSeasonHook hook : hooks) {
            float seasonal = Math.max(0f, hook.getSeasonalDecayModifier(valueKey, MarieSeasonHook.Season.SPRING));
            rate *= seasonal;
        }
        return rate;
    }

    private record DiminishingReturnsConfigOrSkip(
            dev.marie.framework.tracking.DiminishingReturnsConfig config,
            boolean skipDecay
    ) {}
}
