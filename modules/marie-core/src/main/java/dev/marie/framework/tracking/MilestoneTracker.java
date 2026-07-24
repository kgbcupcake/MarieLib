package dev.marie.framework.tracking;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marie.MarieEvents;
import dev.marie.framework.api.progression.MilestoneDefinition;
import dev.marie.framework.api.registry.MilestoneRegistry;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.core.MarieCore;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Accumulates per-value cumulative intake, detects milestone completions, and grants rewards.
 */
@ApiStatus.Internal
public final class MilestoneTracker {

    private MilestoneTracker() {}

    /**
     * Records positive value intake and triggers any newly completed milestones.
     *
     * @param player   the player who received the value
     * @param valueKey the value key that gained intake
     * @param amount   the applied delta (must be positive to count)
     */
    public static void onValueApplied(ServerPlayer player, String valueKey, float amount) {
        if (!FeatureFlagCache.enableMilestones()) {
            return;
        }
        if (amount <= 0f) {
            return;
        }

        MilestoneProgressData data = MilestoneProgressAttachment.getData(player);
        if (data == null) {
            return;
        }

        data.addIntake(valueKey, amount);
        float cumulative = data.getCumulativeIntake(valueKey);

        for (MilestoneDefinition milestone : MilestoneRegistry.getForValue(valueKey)) {
            if (cumulative < milestone.getCumulativeGoal()) {
                continue;
            }
            if (data.isCompleted(milestone.getId())) {
                continue;
            }

            data.markCompleted(milestone.getId());
            applyRewardEffect(player, milestone);
            awardAdvancement(player, milestone);
            NeoForge.EVENT_BUS.post(new MarieEvents.MilestoneTriggeredEvent(
                    player, milestone, valueKey, cumulative));
        }

        for (MilestoneDefinition milestone : MilestoneRegistry.getForAll()) {
            if (data.isCompleted(milestone.getId())) {
                continue;
            }

            List<String> valueKeys = MarieContext.get().valueKeys();
            if (valueKeys.isEmpty()) {
                continue;
            }

            float minCumulative = Float.MAX_VALUE;
            boolean allPass = true;
            for (String key : valueKeys) {
                float keyCumulative = data.getCumulativeIntake(key);
                if (keyCumulative < milestone.getCumulativeGoal()) {
                    allPass = false;
                    break;
                }
                minCumulative = Math.min(minCumulative, keyCumulative);
            }
            if (!allPass) {
                continue;
            }

            data.markCompleted(milestone.getId());
            applyRewardEffect(player, milestone);
            awardAdvancement(player, milestone);
            NeoForge.EVENT_BUS.post(new MarieEvents.MilestoneTriggeredEvent(
                    player, milestone, "all", minCumulative));
        }
    }

    private static void applyRewardEffect(ServerPlayer player, MilestoneDefinition milestone) {
        ResourceLocation effectId = milestone.getRewardEffectId();
        if (effectId == null) {
            return;
        }
        BuiltInRegistries.MOB_EFFECT.getHolder(effectId).ifPresentOrElse(
                holder -> player.addEffect(new MobEffectInstance(
                        holder,
                        milestone.getRewardDuration(),
                        milestone.getRewardAmplifier())),
                () -> MarieCore.LOGGER.warn(
                        "[MarieLib] Milestone '{}' references unknown effect '{}'",
                        milestone.getId(),
                        effectId));
    }

    private static void awardAdvancement(ServerPlayer player, MilestoneDefinition milestone) {
        ResourceLocation advancementId = milestone.getAdvancementId();
        if (advancementId == null) {
            return;
        }
        AdvancementHolder holder = resolveAdvancement(player, advancementId);
        if (holder == null) {
            MarieCore.LOGGER.warn(
                    "[MarieLib] Milestone '{}' references unknown advancement '{}'",
                    milestone.getId(),
                    advancementId);
            return;
        }
        for (String criterion : holder.value().criteria().keySet()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    @Nullable
    private static AdvancementHolder resolveAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        if (player.getServer() == null) {
            return null;
        }
        return player.getServer().getAdvancements().get(advancementId);
    }
}
