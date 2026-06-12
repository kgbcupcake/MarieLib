package dev.marie.MariesLib.api;

import dev.marie.MariesLib.api.ApiStatus;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Defines a named tracking archetype with custom thresholds, decay rates,
 * and bonus effects per profile.
 *
 * <p>Mods or modpacks can register profiles that players can switch between,
 * altering the value system's behaviour to match a tracking philosophy.</p>
 *
 * <p>Use the {@link Builder} to construct instances and register them via
 * {@link MarieAPI#registerTrackingProfile(ProfileDefinition)}.</p>
 */
@ApiStatus.Stable
public final class ProfileDefinition {

    private final String id;
    private final String displayName;
    private final Map<String, Float> customThresholds;
    private final Map<String, Float> customDecayRates;
    private final List<ResourceLocation> bonusEffects;
    @Nullable
    private final String description;

    private ProfileDefinition(
            String id,
            String displayName,
            Map<String, Float> customThresholds,
            Map<String, Float> customDecayRates,
            List<ResourceLocation> bonusEffects,
            @Nullable String description
    ) {
        this.id = id;
        this.displayName = displayName;
        this.customThresholds = Collections.unmodifiableMap(customThresholds);
        this.customDecayRates = Collections.unmodifiableMap(customDecayRates);
        this.bonusEffects = Collections.unmodifiableList(bonusEffects);
        this.description = description;
    }

    /**
     * Creates a new builder for a tracking profile with the given identifier.
     *
     * @param id the unique internal key for this profile (e.g. "profile_a")
     * @return a new {@link Builder} instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Returns the unique internal identifier for this tracking profile.
     *
     * @return the profile key string
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the human-readable display name for this tracking profile.
     *
     * @return the display name (e.g. "Profile A")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns custom value thresholds overriding the defaults when this profile
     * is active. Keys are value identifiers, values are threshold overrides.
     *
     * @return an unmodifiable map of value key to threshold value
     */
    public Map<String, Float> getCustomThresholds() {
        return customThresholds;
    }

    /**
     * Returns custom decay rate overrides for values when this profile is active.
     *
     * @return an unmodifiable map of value key to decay rate override
     */
    public Map<String, Float> getCustomDecayRates() {
        return customDecayRates;
    }

    /**
     * Returns the list of bonus effects granted while this tracking profile is active
     * and the player is meeting the profile's requirements.
     *
     * @return an unmodifiable list of effect {@link ResourceLocation} identifiers
     */
    public List<ResourceLocation> getBonusEffects() {
        return bonusEffects;
    }

    /**
     * Returns an optional description of this tracking profile for display in UIs.
     *
     * @return the profile description, or {@code null} if not set
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Builder for constructing {@link ProfileDefinition} instances.
     */
    @ApiStatus.Stable
    public static final class Builder {

        private final String id;
        private String displayName = "";
        private final Map<String, Float> customThresholds = new java.util.HashMap<>();
        private final Map<String, Float> customDecayRates = new java.util.HashMap<>();
        private final List<ResourceLocation> bonusEffects = new java.util.ArrayList<>();
        @Nullable
        private String description;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the human-readable display name for this profile.
         *
         * @param displayName the display name
         * @return this builder for chaining
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets a custom threshold override for a specific value under this profile.
         *
         * @param valueKey the value identifier
         * @param threshold   the threshold value (0.0 to 1.0)
         * @return this builder for chaining
         */
        public Builder customThreshold(String valueKey, float threshold) {
            this.customThresholds.put(valueKey, threshold);
            return this;
        }

        /**
         * Sets a custom decay rate override for a specific value under this profile.
         *
         * @param valueKey the value identifier
         * @param decayRate   the decay rate per tick
         * @return this builder for chaining
         */
        public Builder customDecayRate(String valueKey, float decayRate) {
            this.customDecayRates.put(valueKey, decayRate);
            return this;
        }

        /**
         * Adds a bonus effect granted when this profile is active and satisfied.
         *
         * @param effectId the effect's {@link ResourceLocation}
         * @return this builder for chaining
         */
        public Builder addBonusEffect(ResourceLocation effectId) {
            this.bonusEffects.add(effectId);
            return this;
        }

        /**
         * Sets an optional description for this tracking profile.
         *
         * @param description a brief text description
         * @return this builder for chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns the immutable {@link ProfileDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        public ProfileDefinition build() {
            if (id == null) {
                throw new IllegalStateException("id is required");
            }
            if (displayName == null) {
                throw new IllegalStateException("displayName is required");
            }
            if (customThresholds == null) {
                throw new IllegalStateException("customThresholds is required");
            }
            if (customDecayRates == null) {
                throw new IllegalStateException("customDecayRates is required");
            }
            if (bonusEffects == null) {
                throw new IllegalStateException("bonusEffects is required");
            }
            return new ProfileDefinition(
                    id,
                    displayName,
                    customThresholds,
                    customDecayRates,
                    bonusEffects,
                    description
            );
        }
    }
}
