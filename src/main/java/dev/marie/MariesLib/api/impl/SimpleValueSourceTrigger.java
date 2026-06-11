package dev.marie.MariesLib.api.impl;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ValueSourceTrigger;

@ApiStatus.Internal
public record SimpleValueSourceTrigger(
        ValueSourceTrigger.TriggerType type,
        String sourceId,
        double payload,
        String triggerId
) implements ValueSourceTrigger {}
