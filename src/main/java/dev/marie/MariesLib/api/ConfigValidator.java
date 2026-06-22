package dev.marie.MariesLib.api;

import dev.marie.MariesLib.config.validation.ValidationResult;

/**
 * A consuming-mod-provided validator that checks one config file or config domain.
 * MarieLib runs registered validators and collects {@link ValidationResult} instances;
 * the consuming mod decides what constitutes a valid config.
 *
 * <p>Register via {@link MarieAPI#registerConfigValidator}.</p>
 *
 * <p>{@link #modId()} identifies which mod owns this validator. It is used by scoped validate
 * commands — {@code /marieslib validate <modid>} and {@code /<modid> validate} — to filter to
 * that mod's validators only. Bare {@code /marieslib validate} with no mod id is not a runnable
 * command.</p>
 */
@ApiStatus.Stable
public interface ConfigValidator {

    /**
     * The mod id of the consuming mod that registered this validator (e.g. {@code "nourished"}).
     */
    String modId();

    /**
     * A unique identifier for this validator (e.g. {@code "nourished_colors"}).
     */
    String validatorId();

    /**
     * Runs the check and returns a structured result with any findings.
     */
    ValidationResult validate();
}
