package dev.marie.framework.api.impl;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ValueSourceTrigger;

@ApiStatus.Internal
public record SimpleValueSourceTrigger(
        ValueSourceTrigger.TriggerType type,
        String sourceId,
        double payload,
        String triggerId
) implements ValueSourceTrigger {}
