package dev.marie.MariesLib.config.validation;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * A single validation finding produced by a {@link dev.marie.MariesLib.api.ConfigValidator}.
 *
 * @param severity the severity level of this finding
 * @param file     the config file name the finding pertains to
 * @param key      the config key within that file
 * @param message  human-readable description of the issue
 */
@ApiStatus.Stable
public record Finding(ValidationResult.Status severity, String file, String key, String message) {}
