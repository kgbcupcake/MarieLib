package dev.marie.MariesLib.config.validation;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ConfigValidator;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.registry.ListRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Internal storage for config validators registered via the public API.
 */
@ApiStatus.Internal
public final class ConfigValidatorRegistry {

    private static final ListRegistry<ConfigValidator> REGISTRY =
            new ListRegistry<>("ConfigValidatorRegistry", null);

    private ConfigValidatorRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        REGISTRY.reset();
    }

    public static void register(ConfigValidator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("validator cannot be null");
        }
        String id = validator.validatorId();
        for (ConfigValidator existing : REGISTRY.values()) {
            if (existing.validatorId().equals(id)) {
                MariesLib.LOGGER.warn("[ConfigValidatorRegistry] Ignoring duplicate validator registration: {}", id);
                return;
            }
        }
        REGISTRY.register(validator);
    }

    public static List<ConfigValidator> getAll() {
        return REGISTRY.values();
    }

    public static List<ConfigValidator> getForMod(String modId) {
        if (modId == null) {
            return List.of();
        }
        List<ConfigValidator> filtered = new ArrayList<>();
        for (ConfigValidator validator : REGISTRY.values()) {
            if (modId.equals(validator.modId())) {
                filtered.add(validator);
            }
        }
        return List.copyOf(filtered);
    }

    public static Set<String> getRegisteredModIds() {
        LinkedHashSet<String> modIds = new LinkedHashSet<>();
        for (ConfigValidator validator : REGISTRY.values()) {
            modIds.add(validator.modId());
        }
        return Collections.unmodifiableSet(modIds);
    }
}
