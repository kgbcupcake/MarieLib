package dev.marie.framework.api.effects;

import dev.marie.framework.api.marieapi.MarieAPI;

import dev.marie.framework.api.ApiStatus;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Defines a synergy interaction between two values. When both values
 * satisfy their respective conditions simultaneously, a bonus or penalty
 * effect is applied to the player.
 *
 * <p>Use the {@link Builder} to construct instances and register them
 * via {@link MarieAPI#registerValueSynergy(SynergyDefinition)}.</p>
 */
@ApiStatus.Stable
public final class SynergyDefinition {

    /**
     * Defines whether the value condition requires a high or low level.
     */
    public enum LevelCondition {
        /** The value must be above its excess threshold. */
        HIGH,
        /** The value must be below its low threshold. */
        LOW,
        /** The value must be in its optimal range (between low and excess). */
        OPTIMAL
    }

    private final String id;
    private final String valueKeyA;
    private final LevelCondition conditionA;
    private final String valueKeyB;
    private final LevelCondition conditionB;
    @Nullable
    private final ResourceLocation bonusEffectId;
    private final int effectAmplifier;
    private final int effectDuration;
    private final boolean isPenalty;

    private SynergyDefinition(
            String id,
            String valueKeyA,
            LevelCondition conditionA,
            String valueKeyB,
            LevelCondition conditionB,
            @Nullable ResourceLocation bonusEffectId,
            int effectAmplifier,
            int effectDuration,
            boolean isPenalty
    ) {
        this.id = id;
        this.valueKeyA = valueKeyA;
        this.conditionA = conditionA;
        this.valueKeyB = valueKeyB;
        this.conditionB = conditionB;
        this.bonusEffectId = bonusEffectId;
        this.effectAmplifier = effectAmplifier;
        this.effectDuration = effectDuration;
        this.isPenalty = isPenalty;
    }

    /**
     * Creates a new builder for a value synergy definition.
     *
     * @param id the unique identifier for this synergy (e.g. "synergy_id")
     * @return a new {@link Builder} instance
     */
    @ApiStatus.Stable
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Returns the unique identifier of this synergy definition.
     *
     * @return the synergy id string
     */
    @ApiStatus.Stable
    public String getId() {
        return id;
    }

    /**
     * Returns the key of the first value in this synergy pair.
     *
     * @return the first value key
     */
    @ApiStatus.Stable
    public String getValueKeyA() {
        return valueKeyA;
    }

    /**
     * Returns the level condition required for the first value.
     *
     * @return the {@link LevelCondition} for value A
     */
    @ApiStatus.Stable
    public LevelCondition getConditionA() {
        return conditionA;
    }

    /**
     * Returns the key of the second value in this synergy pair.
     *
     * @return the second value key
     */
    @ApiStatus.Stable
    public String getValueKeyB() {
        return valueKeyB;
    }

    /**
     * Returns the level condition required for the second value.
     *
     * @return the {@link LevelCondition} for value B
     */
    @ApiStatus.Stable
    public LevelCondition getConditionB() {
        return conditionB;
    }

    /**
     * Returns the mob effect to apply when the synergy conditions are met,
     * or {@code null} if this synergy uses a custom reward mechanism.
     *
     * @return the effect's {@link ResourceLocation}, or {@code null}
     */
    @Nullable
    @ApiStatus.Stable
    public ResourceLocation getBonusEffectId() {
        return bonusEffectId;
    }

    /**
     * Returns the amplifier level of the synergy effect.
     *
     * @return the effect amplifier (0-indexed)
     */
    @ApiStatus.Stable
    public int getEffectAmplifier() {
        return effectAmplifier;
    }

    /**
     * Returns the duration of the synergy effect in game ticks.
     *
     * @return the effect duration in ticks
     */
    @ApiStatus.Stable
    public int getEffectDuration() {
        return effectDuration;
    }

    /**
     * Returns whether this synergy represents a penalty rather than a bonus.
     *
     * @return {@code true} if this is a penalty synergy
     */
    @ApiStatus.Stable
    public boolean isPenalty() {
        return isPenalty;
    }

    /**
     * Builder for constructing {@link SynergyDefinition} instances.
     */
    @ApiStatus.Stable
    public static final class Builder {

        private final String id;
        private String valueKeyA;
        private LevelCondition conditionA = LevelCondition.HIGH;
        private String valueKeyB;
        private LevelCondition conditionB = LevelCondition.HIGH;
        @Nullable
        private ResourceLocation bonusEffectId;
        private int effectAmplifier = 0;
        private int effectDuration = 200;
        private boolean isPenalty = false;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the first value and its required condition.
         *
         * @param valueKey the first value key (e.g. "value_a")
         * @param condition   the level condition required
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder valueA(String valueKey, LevelCondition condition) {
            this.valueKeyA = valueKey;
            this.conditionA = condition;
            return this;
        }

        /**
         * Sets the second value and its required condition.
         *
         * @param valueKey the second value key (e.g. "value_b")
         * @param condition   the level condition required
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder valueB(String valueKey, LevelCondition condition) {
            this.valueKeyB = valueKey;
            this.conditionB = condition;
            return this;
        }

        /**
         * Sets the mob effect granted when both synergy conditions are satisfied.
         *
         * @param effectId the effect's {@link ResourceLocation}
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder bonusEffect(ResourceLocation effectId) {
            this.bonusEffectId = effectId;
            return this;
        }

        /**
         * Sets the amplifier for the synergy effect.
         *
         * @param amplifier the effect amplifier (0-indexed)
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder effectAmplifier(int amplifier) {
            this.effectAmplifier = amplifier;
            return this;
        }

        /**
         * Sets the duration of the synergy effect.
         *
         * @param duration the effect duration in game ticks
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder effectDuration(int duration) {
            this.effectDuration = duration;
            return this;
        }

        /**
         * Marks this synergy as a penalty (negative effect) rather than a bonus.
         *
         * @param penalty {@code true} if this synergy applies a penalty
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder penalty(boolean penalty) {
            this.isPenalty = penalty;
            return this;
        }

        /**
         * Builds and returns the immutable {@link SynergyDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        @ApiStatus.Stable
        public SynergyDefinition build() {
            if (id == null) {
                throw new IllegalStateException("id is required");
            }
            if (valueKeyA == null) {
                throw new IllegalStateException("valueKeyA is required");
            }
            if (conditionA == null) {
                throw new IllegalStateException("conditionA is required");
            }
            if (valueKeyB == null) {
                throw new IllegalStateException("valueKeyB is required");
            }
            if (conditionB == null) {
                throw new IllegalStateException("conditionB is required");
            }
            return new SynergyDefinition(
                    id,
                    valueKeyA,
                    conditionA,
                    valueKeyB,
                    conditionB,
                    bonusEffectId,
                    effectAmplifier,
                    effectDuration,
                    isPenalty
            );
        }
    }
}
