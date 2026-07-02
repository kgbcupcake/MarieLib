package dev.marie.framework.api;

import dev.marie.framework.config.validation.ValidationResult;

/**
 * Validates a consumer mod's configuration and returns a structured result.
 * Register via {@link MarieAPI#registerConfigValidator(ConfigValidator)}.
 */
@ApiStatus.Stable
public interface ConfigValidator {

    /** The mod this validator belongs to. Used for grouping in command output. */
    String modId();

    /** Unique stable identifier for this validator instance. */
    String validatorId();

    /** Runs validation and returns the result. Must not throw. */
    ValidationResult validate();
}
