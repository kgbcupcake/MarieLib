package dev.marie.framework.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.SleepBonusEvaluator;
import dev.marie.framework.api.registry.SleepBonusEvaluatorRegistry;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.tracking.TrackingData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.Collections;
import java.util.Map;

@ApiStatus.Internal
public class RestCycleListener {

    @SubscribeEvent
    public void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!FeatureFlagCache.enableSleepBonus()) return;
        if (!TrackingAttachment.isRegistered()) return;

        var evaluators = SleepBonusEvaluatorRegistry.getAll();
        if (evaluators.isEmpty()) return;

        TrackingData tracking = TrackingAttachment.getData(player);
        Map<String, Float> values = Collections.unmodifiableMap(tracking.values);

        for (SleepBonusEvaluator evaluator : evaluators) {
            try {
                MobEffectInstance effect = evaluator.evaluate(player, values);
                if (effect != null) {
                    player.addEffect(effect);
                }
            } catch (Exception ex) {
                MarieCore.LOGGER.warn(
                        "[MarieLib] SleepBonusEvaluator threw during evaluate(): {}",
                        ex.getMessage());
            }
        }
    }
}
