package dev.marie.framework.api.effects;

import dev.marie.framework.api.marieapi.MarieAPI;

import dev.marie.framework.api.ApiStatus;

import net.minecraft.resources.ResourceLocation;

/**
 * Defines a status effect that should be applied when a value crosses
 * a specified threshold boundary.
 *
 * <p>Use the {@link Builder} to construct instances and register them
 * via {@link MarieAPI#registerCustomEffect(ThresholdEffect)}.</p>
 */
@ApiStatus.Stable
public final class ThresholdEffect {

    /**
     * Classifies the type of threshold crossing that triggers an effect.
     */
    public enum ThresholdType {
        /** Value has dropped below the critical threshold. */
        CRITICAL,
        /** Value has dropped below the low threshold. */
        LOW,
        /** Value has exceeded the excess threshold. */
        EXCESS,
        /** Value is within a bonus range (all thresholds satisfied optimally). */
        BONUS
    }

    private final String valueKey;
    private final float threshold;
    private final ThresholdType thresholdType;
    private final ResourceLocation effectId;
    private final int amplifier;
    private final int duration;

    private ThresholdEffect(
            String valueKey,
            float threshold,
            ThresholdType thresholdType,
            ResourceLocation effectId,
            int amplifier,
            int duration
    ) {
        this.valueKey = valueKey;
        this.threshold = threshold;
        this.thresholdType = thresholdType;
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.duration = duration;
    }

    /**
     * Creates a new builder for an effect definition.
     *
     * @return a new {@link Builder} instance
     */
    @ApiStatus.Stable
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the value key this effect is bound to.
     *
     * @return the value identifier string (e.g. "value_a")
     */
    @ApiStatus.Stable
    public String getValueKey() {
        return valueKey;
    }

    /**
     * Returns the numeric threshold value at which this effect activates.
     *
     * @return the threshold as a normalized float (0.0 to 1.0)
     */
    @ApiStatus.Stable
    public float getThreshold() {
        return threshold;
    }

    /**
     * Returns the type of threshold crossing that triggers this effect.
     *
     * @return the {@link ThresholdType} enum value
     */
    @ApiStatus.Stable
    public ThresholdType getThresholdType() {
        return thresholdType;
    }

    /**
     * Returns the registry identifier of the mob effect to apply.
     *
     * @return the effect's {@link ResourceLocation}
     */
    @ApiStatus.Stable
    public ResourceLocation getEffectId() {
        return effectId;
    }

    /**
     * Returns the amplifier level for the applied effect (0-indexed).
     *
     * @return the effect amplifier
     */
    @ApiStatus.Stable
    public int getAmplifier() {
        return amplifier;
    }

    /**
     * Returns the duration in ticks for the applied effect.
     *
     * @return the effect duration in game ticks
     */
    @ApiStatus.Stable
    public int getDuration() {
        return duration;
    }

    /**
     * Builder for constructing {@link ThresholdEffect} instances.
     */
    @ApiStatus.Stable
    public static final class Builder {

        private String valueKey;
        private float threshold;
        private ThresholdType thresholdType;
        private ResourceLocation effectId;
        private int amplifier;
        private int duration;

        private Builder() {}

        /**
         * Sets the value key this effect is bound to.
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
         * Sets the numeric threshold that triggers this effect.
         *
         * @param threshold the activation threshold (0.0 to 1.0)
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder threshold(float threshold) {
            this.threshold = threshold;
            return this;
        }

        /**
         * Sets the type of threshold crossing that activates this effect.
         *
         * @param type the {@link ThresholdType} enum value
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder thresholdType(ThresholdType type) {
            this.thresholdType = type;
            return this;
        }

        /**
         * Sets the registry identifier of the mob effect to apply.
         *
         * @param effectId the effect's {@link ResourceLocation}
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder effectId(ResourceLocation effectId) {
            this.effectId = effectId;
            return this;
        }

        /**
         * Sets the amplifier level for the applied effect.
         *
         * @param amplifier the effect amplifier (0-indexed)
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder amplifier(int amplifier) {
            this.amplifier = amplifier;
            return this;
        }

        /**
         * Sets the duration for the applied effect in game ticks.
         *
         * @param duration the effect duration in ticks
         * @return this builder for chaining
         */
        @ApiStatus.Stable
        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Builds and returns the immutable {@link ThresholdEffect}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        @ApiStatus.Stable
        public ThresholdEffect build() {
            if (valueKey == null) {
                throw new IllegalStateException("valueKey is required");
            }
            if (thresholdType == null) {
                throw new IllegalStateException("thresholdType is required");
            }
            if (effectId == null) {
                throw new IllegalStateException("effectId is required");
            }
            return new ThresholdEffect(valueKey, threshold, thresholdType, effectId, amplifier, duration);
        }
    }
}
