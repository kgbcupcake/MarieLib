package dev.marie.MariesLib.config.validation;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ConfigValidator;
import dev.marie.MariesLib.config.ConfigValidatorRegistry;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Stable
public final class ValidationRunner {

    private ValidationRunner() {}

    public static List<ValidationResult> runForMod(String modId) {
        List<ValidationResult> results = new ArrayList<>();
        for (ConfigValidator validator : ConfigValidatorRegistry.getAllRaw()) {
            if (!validator.modId().equals(modId)) {
                continue;
            }
            try {
                results.add(validator.validate());
            } catch (Exception e) {
                results.add(new ValidationResult(
                        validator.validatorId(),
                        ValidationResult.Status.FAIL,
                        List.of(new Finding(ValidationResult.Status.FAIL, "unknown", validator.validatorId(),
                                "Validator threw exception: " + e.getMessage()))));
            }
        }
        return results;
    }

    public static List<ValidationResult> runAll() {
        List<ValidationResult> results = new ArrayList<>();
        for (ConfigValidator validator : ConfigValidatorRegistry.getAllRaw()) {
            try {
                results.add(validator.validate());
            } catch (Exception e) {
                results.add(new ValidationResult(
                        validator.validatorId(),
                        ValidationResult.Status.FAIL,
                        List.of(new Finding(ValidationResult.Status.FAIL, "unknown", validator.validatorId(),
                                "Validator threw exception: " + e.getMessage()))));
            }
        }
        return results;
    }
}
