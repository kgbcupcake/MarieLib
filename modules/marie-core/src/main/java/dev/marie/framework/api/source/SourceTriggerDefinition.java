package dev.marie.framework.api.source;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marieapi.MarieAPI;
import dev.marie.framework.api.value.ValueSourceTrigger;

/**
 * Declares that a non-item source key contributes a fixed amount
 * to a value when the matching trigger type fires.
 *
 * <p>Register via {@link MarieAPI#registerTriggerSource(SourceTriggerDefinition)}.</p>
 */
@ApiStatus.Stable
public final class SourceTriggerDefinition {

    private final String sourceKey;
    private final ValueSourceTrigger.TriggerType triggerType;
    private final String valueKey;
    private final float amount;

    private SourceTriggerDefinition(String sourceKey,
                                    ValueSourceTrigger.TriggerType triggerType,
                                    String valueKey,
                                    float amount) {
        this.sourceKey = sourceKey;
        this.triggerType = triggerType;
        this.valueKey = valueKey;
        this.amount = amount;
    }

    public String getSourceKey() { return sourceKey; }
    public ValueSourceTrigger.TriggerType getTriggerType() { return triggerType; }
    public String getValueKey() { return valueKey; }
    public float getAmount() { return amount; }

    public static Builder builder(String sourceKey) {
        return new Builder(sourceKey);
    }

    public static final class Builder {
        private final String sourceKey;
        private ValueSourceTrigger.TriggerType triggerType = ValueSourceTrigger.TriggerType.CUSTOM;
        private String valueKey;
        private float amount;

        private Builder(String sourceKey) {
            this.sourceKey = sourceKey;
        }

        public Builder triggerType(ValueSourceTrigger.TriggerType type) {
            this.triggerType = type;
            return this;
        }

        public Builder valueKey(String key) {
            this.valueKey = key;
            return this;
        }

        public Builder amount(float amount) {
            this.amount = amount;
            return this;
        }

        public SourceTriggerDefinition build() {
            if (sourceKey == null || sourceKey.isBlank()) {
                throw new IllegalStateException("sourceKey is required");
            }
            if (valueKey == null || valueKey.isBlank()) {
                throw new IllegalStateException("valueKey is required");
            }
            if (!Float.isFinite(amount)) {
                throw new IllegalStateException("amount must be finite");
            }
            return new SourceTriggerDefinition(sourceKey, triggerType, valueKey, amount);
        }
    }
}
