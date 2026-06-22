package dev.marie.MariesLib.config.validation;

import javax.annotation.Nullable;

public record Finding(ValidationResult.Status severity, String file, @Nullable String key, String message) {}
