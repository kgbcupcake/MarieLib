package dev.marie.MariesLib.config.validation;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ConfigValidator;
import dev.marie.MariesLib.core.MariesLib;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public final class ValidationRunner {

    private ValidationRunner() {}

    public static List<ValidationResult> runAll() {
        return run(ConfigValidatorRegistry.getAll(), null);
    }

    public static List<ValidationResult> runForMod(String modId) {
        return run(ConfigValidatorRegistry.getForMod(modId), modId);
    }

    private static List<ValidationResult> run(List<ConfigValidator> validators, String modId) {
        List<ValidationResult> results = new ArrayList<>();
        int pass = 0;
        int warn = 0;
        int fail = 0;

        for (ConfigValidator validator : validators) {
            String validatorId = validator.validatorId();
            ValidationResult result;
            try {
                result = validator.validate();
            } catch (RuntimeException e) {
                MariesLib.LOGGER.error("[ValidationRunner] Validator {} threw an exception", validatorId, e);
                String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                result = new ValidationResult(
                        validatorId,
                        ValidationResult.Status.FAIL,
                        List.of(new Finding(ValidationResult.Status.FAIL, "", null, "Validator threw: " + message))
                );
            }
            results.add(result);
            switch (result.status()) {
                case PASS -> pass++;
                case WARN -> warn++;
                case FAIL -> fail++;
            }
        }

        if (modId != null) {
            MariesLib.LOGGER.info("[ValidationRunner] Completed for mod {}: {} PASS, {} WARN, {} FAIL", modId, pass, warn, fail);
        } else {
            MariesLib.LOGGER.info("[ValidationRunner] Completed: {} PASS, {} WARN, {} FAIL", pass, warn, fail);
        }
        return List.copyOf(results);
    }
}
