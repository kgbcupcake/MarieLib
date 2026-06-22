package dev.marie.MariesLib.config.validation;

import java.util.List;

public record ValidationResult(String validatorId, Status status, List<Finding> findings) {

    public enum Status {
        PASS,
        WARN,
        FAIL
    }

    public static ValidationResult pass(String validatorId) {
        return new ValidationResult(validatorId, Status.PASS, List.of());
    }
}
