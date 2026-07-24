package dev.marie.framework.api.progression;

import dev.marie.framework.api.marieapi.MarieAPI;

import dev.marie.framework.api.ApiStatus;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Defines a milestone that fires once when a player reaches a cumulative
 * value goal. Functions as an achievement system built into the API.
 *
 * <p>Milestones are persistent across sessions and fire exactly once per player.
 * They can grant effects, trigger advancements, or invoke custom callbacks.</p>
 *
 * <p>Use the {@link Builder} to construct instances and register them
 * via {@link MarieAPI#registerMilestone(MilestoneDefinition)}.</p>
 */
@ApiStatus.Stable
public final class MilestoneDefinition {

    private final String id;
    private final String valueKey;
    private final float cumulativeGoal;
    @Nullable
    private final ResourceLocation rewardEffectId;
    private final int rewardAmplifier;
    private final int rewardDuration;
    @Nullable
    private final ResourceLocation advancementId;

    private MilestoneDefinition(
            String id,
            String valueKey,
            float cumulativeGoal,
            @Nullable ResourceLocation rewardEffectId,
            int rewardAmplifier,
            int rewardDuration,
            @Nullable ResourceLocation advancementId
    ) {
        this.id = id;
        this.valueKey = valueKey;
        this.cumulativeGoal = cumulativeGoal;
        this.rewardEffectId = rewardEffectId;
        this.rewardAmplifier = rewardAmplifier;
        this.rewardDuration = rewardDuration;
        this.advancementId = advancementId;
    }

    /**
     * Creates a new builder for a value milestone.
     *
     * @param id the unique identifier for this milestone (e.g. "milestone_id")
     * @return a new {@link Builder} instance
     */
    @ApiStatus.Stable
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Returns the unique identifier of this milestone.
     *
     * @return the milestone id string
     */
    @ApiStatus.Stable
    public String getId() {
        return id;
    }

    /**
     * Returns the value key this milestone tracks.
     *
     * @return the value identifier string
     */
    @ApiStatus.Stable
    public String getValueKey() {
        return valueKey;
    }

    /**
     * Returns the cumulative value amount required to trigger this milestone.
     *
     * @return the cumulative goal value
     */
    @ApiStatus.Stable
    public float getCumulativeGoal() {
        return cumulativeGoal;
    }

    /**
     * Returns the mob effect granted as a reward when the milestone triggers,
     * or {@code null} if no effect reward is configured.
     *
     * @return the reward effect's {@link ResourceLocation}, or {@code null}
     */
    @Nullable
    @ApiStatus.Stable
    public ResourceLocation getRewardEffectId() {
        return rewardEffectId;
    }

    /**
     * Returns the amplifier for the reward effect.
     *
     * @return the effect amplifier (0-indexed)
     */
    @ApiStatus.Stable
    public int getRewardAmplifier() {
        return rewardAmplifier;
    }

    /**
     * Returns the duration of the reward effect in game ticks.
     *
     * @return the reward duration in ticks
     */
    @ApiStatus.Stable
    public int getRewardDuration() {
        return rewardDuration;
    }

    /**
     * Returns the advancement to grant when this milestone triggers,
     * or {@code null} if no advancement is configured.
     *
     * @return the advancement's {@link ResourceLocation}, or {@code null}
     */
    @Nullable
    @ApiStatus.Stable
    public ResourceLocation getAdvancementId() {
        return advancementId;
    }

    /**
     * Builder for constructing {@link MilestoneDefinition} instances.
     */
    @ApiStatus.Stable
    public static final class Builder {

        private final String id;
        private String valueKey;
        private float cumulativeGoal;
        @Nullable
        private ResourceLocation rewardEffectId;
        private int rewardAmplifier = 0;
        private int rewardDuration = 200;
        @Nullable
        private ResourceLocation advancementId;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the value key this milestone tracks.
         *
         * @param valueKey the value identifier (e.g. "value_a")
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder valueKey(String valueKey) {
            this.valueKey = valueKey;
            return this;
        }

        /**
         * Sets the cumulative value amount required to trigger this milestone.
         *
         * @param goal the cumulative goal value
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder cumulativeGoal(float goal) {
            this.cumulativeGoal = goal;
            return this;
        }

        /**
         * Sets the mob effect granted as a one-time reward.
         *
         * @param effectId the reward effect's {@link ResourceLocation}
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder rewardEffect(ResourceLocation effectId) {
            this.rewardEffectId = effectId;
            return this;
        }

        /**
         * Sets the amplifier for the reward effect.
         *
         * @param amplifier the effect amplifier (0-indexed)
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder rewardAmplifier(int amplifier) {
            this.rewardAmplifier = amplifier;
            return this;
        }

        /**
         * Sets the duration for the reward effect.
         *
         * @param duration the duration in game ticks
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder rewardDuration(int duration) {
            this.rewardDuration = duration;
            return this;
        }

        /**
         * Sets an advancement to grant when the milestone triggers.
         *
         * @param advancementId the advancement's {@link ResourceLocation}
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder advancement(ResourceLocation advancementId) {
            this.advancementId = advancementId;
            return this;
        }

        /**
         * Builds and returns the immutable {@link MilestoneDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        @ApiStatus.Stable
        public MilestoneDefinition build() {
            if (id == null) {
                throw new IllegalStateException("id is required");
            }
            if (valueKey == null) {
                throw new IllegalStateException("valueKey is required");
            }
            return new MilestoneDefinition(
                    id,
                    valueKey,
                    cumulativeGoal,
                    rewardEffectId,
                    rewardAmplifier,
                    rewardDuration,
                    advancementId
            );
        }
    }
}
