package dev.marie.MariesLib.api;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Consuming-mod-provided evaluator that determines what happens when
 * a player wakes up based on their current value state.
 *
 * <p>The lib fires this after a player wakes up. The consuming mod
 * decides the threshold, which values matter, and what effect to apply.
 * The lib applies nothing of its own.</p>
 *
 * <p>Register via {@link MarieAPI#registerSleepBonusEvaluator}.</p>
 */
@ApiStatus.Stable
public interface SleepBonusEvaluator {

    /**
     * Called when a player wakes up. Return a non-null effect instance
     * to apply it, or return null to apply nothing.
     *
     * @param player   the player who woke up
     * @param values   an unmodifiable snapshot of the player's current
     *                 value levels, keyed by value key, normalized [0,1]
     * @return a MobEffectInstance to apply, or null
     */
    @Nullable
    MobEffectInstance evaluate(ServerPlayer player, Map<String, Float> values);
}
