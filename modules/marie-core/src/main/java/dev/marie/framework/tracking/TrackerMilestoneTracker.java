package dev.marie.framework.tracking;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marie.MarieEvents;
import dev.marie.framework.api.progression.TrackerMilestoneDefinition;
import dev.marie.framework.api.registry.TrackerMilestoneRegistry;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.tracking.tracker.MarieTracking;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;

/**
 * Accumulates per-tracker lifetime totals, detects tracker milestone completions, and grants
 * rewards. Sibling to {@link MilestoneTracker} for the generic MarieLib tracker system
 * (see {@link MarieTracking}) — fully decoupled from it, with its own storage and events. Not
 * wired into {@link MarieTracking#incrementTracker}; callers invoke {@link #onTrackerIncremented}
 * alongside it.
 */
@ApiStatus.Internal
public final class TrackerMilestoneTracker {

    private TrackerMilestoneTracker() {}

    /**
     * Records tracker intake and triggers any newly completed tracker milestones.
     *
     * @param player    the player who accumulated the tracker value
     * @param trackerId the tracker that gained intake
     * @param amount    the applied delta
     */
    public static void onTrackerIncremented(ServerPlayer player, ResourceLocation trackerId, float amount) {
        TrackerMilestoneProgressData data = TrackerMilestoneProgressAttachment.getData(player);
        if (data == null) {
            return;
        }

        String trackerKey = trackerId.toString();
        data.addLifetime(trackerKey, amount);
        float lifetime = data.getLifetime(trackerKey);

        for (TrackerMilestoneDefinition milestone : TrackerMilestoneRegistry.getForTracker(trackerId)) {
            if (data.isCompleted(milestone.getId())) {
                continue;
            }

            float currentValue = switch (milestone.getScope()) {
                case LIFETIME -> lifetime;
                case CURRENT_PERIOD -> MarieTracking.getCurrentTrackerValue(player, trackerId);
            };

            if (currentValue < milestone.getGoal()) {
                continue;
            }

            data.markCompleted(milestone.getId());
            applyRewardEffect(player, milestone);
            awardAdvancement(player, milestone);
            NeoForge.EVENT_BUS.post(new MarieEvents.TrackerMilestoneTriggeredEvent(
                    player, milestone, trackerId, currentValue));
        }
    }

    private static void applyRewardEffect(ServerPlayer player, TrackerMilestoneDefinition milestone) {
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
                        "[MarieLib] Tracker milestone '{}' references unknown effect '{}'",
                        milestone.getId(),
                        effectId));
    }

    private static void awardAdvancement(ServerPlayer player, TrackerMilestoneDefinition milestone) {
        ResourceLocation advancementId = milestone.getAdvancementId();
        if (advancementId == null) {
            return;
        }
        AdvancementHolder holder = resolveAdvancement(player, advancementId);
        if (holder == null) {
            MarieCore.LOGGER.warn(
                    "[MarieLib] Tracker milestone '{}' references unknown advancement '{}'",
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
