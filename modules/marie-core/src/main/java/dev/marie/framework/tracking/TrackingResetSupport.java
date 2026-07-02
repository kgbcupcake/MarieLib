package dev.marie.framework.tracking;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.IMarieConfig;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.handler.SourceApplicationPipeline;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

@ApiStatus.Internal
public final class TrackingResetSupport {

    private TrackingResetSupport() {}

    /**
     * Resets every registered value bar to {@code fill}. Does not clear application memory.
     *
     * @return {@code true} if any bar changed
     */
    public static boolean resetAllBarValues(ServerPlayer player, TrackingData tracking, float fill) {
        float clamped = Mth.clamp(fill, 0f, 1f);
        boolean changed = false;
        for (String key : MarieContext.get().valueKeys()) {
            if (SourceApplicationPipeline.writeDirectValue(player, tracking, key, clamped)) {
                changed = true;
            }
        }
        return changed;
    }

    /** Clears source/category/family memory and calorie total. */
    public static void clearApplicationMemory(TrackingData tracking) {
        tracking.sourceMemory.clear();
        tracking.categoryMemory.clear();
        tracking.familyMemory.clear();
        tracking.total = 0f;
        tracking.lastTickTime = 0L;
    }

    public static float resolveStartingFill() {
        DiminishingReturnsConfig cfg = IMarieConfig.get().trackingMemoryConfig();
        if (cfg != null) {
            return Mth.clamp((float) cfg.startingValueFill(), 0f, 1f);
        }
        return 0.5f;
    }

    /**
     * Resets all bars to {@code fill} and clears application memory. Used by reset commands
     * and {@link DeathNutritionBehavior#RESET_TO_STARTING} / {@link DeathNutritionBehavior#VANILLA_HALF}.
     *
     * @return {@code true} if any bar changed
     */
    public static boolean resetAllValuesAndMemory(ServerPlayer player, TrackingData tracking, float fill) {
        clearApplicationMemory(tracking);
        return resetAllBarValues(player, tracking, fill);
    }

    /**
     * Applies death respawn policy from {@link MarieContext}. When a custom
     * {@link MarieContext#deathNutritionHandler()} is registered, it fully replaces the enum policy.
     */
    public static void applyDeathNutritionOnRespawn(ServerPlayer player, TrackingData tracking) {
        if (!MarieContext.isRegistered()) {
            return;
        }
        MarieContext ctx = MarieContext.get();

        var custom = ctx.deathNutritionHandler();
        if (custom != null) {
            custom.accept(player, tracking);
            TrackingAttachment.setData(player, tracking);
            return;
        }

        DeathNutritionBehavior behavior = ctx.deathNutritionBehavior().get();
        boolean changed = switch (behavior) {
            case PRESERVE -> false;
            case RESET_TO_STARTING -> resetAllValuesAndMemory(player, tracking, resolveStartingFill());
            case VANILLA_HALF -> resetAllValuesAndMemory(player, tracking, 0.5f);
        };
        if (changed) {
            TrackingAttachment.setData(player, tracking);
        }
    }

    /** Baseline fill for brand-new {@link TrackingData} instances. */
    public static float resolveInitialBarFill() {
        if (MarieContext.isRegistered()) {
            return resolveStartingFill();
        }
        try {
            DiminishingReturnsConfig cfg = IMarieConfig.get().trackingMemoryConfig();
            if (cfg != null) {
                return Mth.clamp((float) cfg.startingValueFill(), 0f, 1f);
            }
        } catch (Exception ignored) {
        }
        return 0.5f;
    }
}
