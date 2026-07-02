package dev.marie.framework.config.validation;

import dev.marie.framework.api.ApiStatus;

import java.util.List;

/**
 * Outcome of a single {@link dev.marie.framework.api.ConfigValidator} run.
 */
@ApiStatus.Stable
public final class ValidationResult {

    public enum Status { PASS, WARN, FAIL }

    private final String validatorId;
    private final Status status;
    private final List<Finding> findings;

    public ValidationResult(String validatorId, Status status, List<Finding> findings) {
        this.validatorId = validatorId;
        this.status = status;
        this.findings = findings;
    }

    public static ValidationResult pass(String validatorId) {
        return new ValidationResult(validatorId, Status.PASS, List.of());
    }

    public String validatorId() { return validatorId; }
    public Status status()      { return status; }
    public List<Finding> findings() { return findings; }
}
