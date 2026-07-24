package dev.marie.framework.api.source;

import dev.marie.framework.api.marieapi.MarieAPI;

import dev.marie.framework.api.ApiStatus;

import net.minecraft.resources.ResourceLocation;

/**
 * Defines a source pair synergy: applying source A within a time window of source B
 * grants a bonus value burst when both sources are consumed in quick succession.
 *
 * <p>Use the {@link Builder} to construct instances and register them
 * via {@link MarieAPI#registerSourcePairSynergy(SourcePairSynergy)}.</p>
 */
@ApiStatus.Stable
public final class SourcePairSynergy {

    private final String id;
    private final ResourceLocation sourceA;
    private final ResourceLocation sourceB;
    private final int timeWindowTicks;
    private final String bonusValueKey;
    private final float bonusAmount;
    private final float valueModifier;
    private final int modifierDurationTicks;

    private SourcePairSynergy(
            String id,
            ResourceLocation sourceA,
            ResourceLocation sourceB,
            int timeWindowTicks,
            String bonusValueKey,
            float bonusAmount,
            float valueModifier,
            int modifierDurationTicks
    ) {
        this.id = id;
        this.sourceA = sourceA;
        this.sourceB = sourceB;
        this.timeWindowTicks = timeWindowTicks;
        this.bonusValueKey = bonusValueKey;
        this.bonusAmount = bonusAmount;
        this.valueModifier = valueModifier;
        this.modifierDurationTicks = modifierDurationTicks;
    }

    /**
     * Creates a new builder for a source synergy definition.
     *
     * @param id the unique identifier for this source synergy (e.g. "source_pair_id")
     * @return a new {@link Builder} instance
     */
    @ApiStatus.Stable
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Returns the unique identifier for this source synergy.
     *
     * @return the synergy id string
     */
    @ApiStatus.Stable
    public String getId() {
        return id;
    }

    /**
     * Returns the first source item in the synergy pair.
     *
     * @return the {@link ResourceLocation} of source A
     */
    @ApiStatus.Stable
    public ResourceLocation getSourceA() {
        return sourceA;
    }

    /**
     * Returns the second source item in the synergy pair.
     *
     * @return the {@link ResourceLocation} of source B
     */
    @ApiStatus.Stable
    public ResourceLocation getSourceB() {
        return sourceB;
    }

    /**
     * Returns the time window in game ticks within which both sources must
     * be consumed for the synergy to activate.
     *
     * @return the time window in ticks (20 ticks = 1 second)
     */
    @ApiStatus.Stable
    public int getTimeWindowTicks() {
        return timeWindowTicks;
    }

    /**
     * Returns the value key that receives the bonus burst when
     * the synergy activates.
     *
     * @return the bonus value key (e.g. "value_a")
     */
    @ApiStatus.Stable
    public String getBonusValueKey() {
        return bonusValueKey;
    }

    /**
     * Returns the bonus value amount added when the synergy activates.
     *
     * @return the bonus amount as a float
     */
    @ApiStatus.Stable
    public float getBonusAmount() {
        return bonusAmount;
    }

    /**
     * Returns the temporary value modifier applied when the synergy activates.
     *
     * @return the value modifier multiplier; {@code 1.0} means no effect
     */
    @ApiStatus.Stable
    public float getValueModifier() {
        return valueModifier;
    }

    /**
     * Returns the duration, in game ticks, that the value modifier remains active.
     *
     * @return the modifier duration in ticks; {@code 0} means the modifier is not applied
     */
    @ApiStatus.Stable
    public int getModifierDurationTicks() {
        return modifierDurationTicks;
    }

    /**
     * Builder for constructing {@link SourcePairSynergy} instances.
     */
    @ApiStatus.Stable
    public static final class Builder {

        private final String id;
        private ResourceLocation sourceA;
        private ResourceLocation sourceB;
        private int timeWindowTicks = 100;
        private String bonusValueKey;
        private float bonusAmount;
        private float valueModifier = 1.0f;
        private int modifierDurationTicks = 0;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the first source item in the synergy pair.
         *
         * @param sourceA the {@link ResourceLocation} of the first source
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder sourceA(ResourceLocation sourceA) {
            this.sourceA = sourceA;
            return this;
        }

        /**
         * Sets the second source item in the synergy pair.
         *
         * @param sourceB the {@link ResourceLocation} of the second source
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder sourceB(ResourceLocation sourceB) {
            this.sourceB = sourceB;
            return this;
        }

        /**
         * Sets the time window within which both sources must be consumed
         * for the synergy to activate.
         *
         * @param ticks the time window in game ticks (20 ticks = 1 second)
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder timeWindowTicks(int ticks) {
            this.timeWindowTicks = ticks;
            return this;
        }

        /**
         * Sets the value key that receives the bonus when activated.
         *
         * @param valueKey the bonus value key (e.g. "value_a")
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder bonusValueKey(String valueKey) {
            this.bonusValueKey = valueKey;
            return this;
        }

        /**
         * Sets the bonus value amount granted on synergy activation.
         *
         * @param amount the bonus amount
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder bonusAmount(float amount) {
            this.bonusAmount = amount;
            return this;
        }

        /**
         * Sets the temporary value modifier applied when the synergy activates.
         *
         * @param valueModifier the value modifier multiplier; {@code 1.0} means no effect
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder valueModifier(float valueModifier) {
            this.valueModifier = valueModifier;
            return this;
        }

        /**
         * Sets the duration, in game ticks, that the value modifier remains active.
         *
         * @param ticks the modifier duration in ticks; {@code 0} disables the modifier
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder modifierDurationTicks(int ticks) {
            this.modifierDurationTicks = ticks;
            return this;
        }

        /**
         * Builds and returns the immutable {@link SourcePairSynergy}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        @ApiStatus.Stable
        public SourcePairSynergy build() {
            if (id == null) {
                throw new IllegalStateException("id is required");
            }
            if (sourceA == null) {
                throw new IllegalStateException("sourceA is required");
            }
            if (sourceB == null) {
                throw new IllegalStateException("sourceB is required");
            }
            if (bonusValueKey == null) {
                throw new IllegalStateException("bonusValueKey is required");
            }
            return new SourcePairSynergy(id, sourceA, sourceB, timeWindowTicks, bonusValueKey, bonusAmount,
                    valueModifier, modifierDurationTicks);
        }
    }
}
