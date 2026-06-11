package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.SleepBonusEvaluator;
import dev.marie.MariesLib.api.registry.SleepBonusEvaluatorRegistry;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.Collections;
import java.util.Map;

@ApiStatus.Internal
public class SleepBonusHandler {

    @SubscribeEvent
    public void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModuleCache.enableSleepBonus) return;
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
                MariesLib.LOGGER.warn(
                        "[MarieLib] SleepBonusEvaluator threw during evaluate(): {}",
                        ex.getMessage());
            }
        }
    }
}
