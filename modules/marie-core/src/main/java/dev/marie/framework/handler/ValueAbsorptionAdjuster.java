package dev.marie.framework.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.effects.AbsorptionModifier;
import dev.marie.framework.api.marie.MarieSeasonHook;
import dev.marie.framework.api.registry.AbsorptionModifierRegistry;
import dev.marie.framework.api.registry.SeasonHookRegistry;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.core.MarieCore;

import net.minecraft.server.level.ServerPlayer;

/**
 * Applies seasonal and registered absorption modifiers to a value delta for
 * {@link SourceApplicationPipeline}, extracted to keep the pipeline's per-value loop free of
 * modifier-resolution detail. Owned exclusively by {@link SourceApplicationPipeline} via
 * composition — not intended to be shared or held elsewhere.
 */
@ApiStatus.Internal
final class ValueAbsorptionAdjuster {

    private ValueAbsorptionAdjuster() {}

    static float applySeasonalAbsorption(ServerPlayer player, String valueKey, float baseAmount) {
        var hooks = SeasonHookRegistry.getAll();
        if (!FeatureFlagCache.enableSeasonHooks() || hooks.isEmpty()) {
            return baseAmount;
        }
        float amount = baseAmount;
        for (MarieSeasonHook hook : hooks) {
            float mult = hook.getSeasonalAbsorptionModifier(valueKey, MarieSeasonHook.Season.SPRING);
            if (!Float.isFinite(mult)) {
                MarieCore.LOGGER.warn("[MarieLib] Seasonal modifier returned non-finite value {} for value={} — using 0", mult, valueKey);
                mult = 0f;
            }
            amount *= Math.max(0f, mult);
        }
        if (!Float.isFinite(amount)) {
            MarieCore.LOGGER.warn("[MarieLib] Seasonal absorption result non-finite for value={} — using 0", valueKey);
            return 0f;
        }
        return amount;
    }

    static float applyAbsorptionModifiers(ServerPlayer player, String valueKey, float baseAmount) {
        var modifiers = AbsorptionModifierRegistry.getAll();
        if (!FeatureFlagCache.enableAbsorptionModifiers() || modifiers.isEmpty()) {
            return baseAmount;
        }
        float amount = baseAmount;
        for (AbsorptionModifier modifier : modifiers) {
            float factor = modifier.getAbsorptionMultiplier(player, valueKey, amount);
            if (!Float.isFinite(factor)) {
                MarieCore.LOGGER.warn("[MarieLib] Absorption modifier returned non-finite value {} for value={} — using 0", factor, valueKey);
                factor = 0f;
            }
            amount *= Math.max(0f, factor);
        }
        if (!Float.isFinite(amount)) {
            MarieCore.LOGGER.warn("[MarieLib] Absorption modifier result non-finite for value={} — using 0", valueKey);
            return 0f;
        }
        return amount;
    }
}
