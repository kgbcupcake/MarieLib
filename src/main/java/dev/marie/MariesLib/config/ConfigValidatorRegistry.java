package dev.marie.MariesLib.config;

import com.mojang.brigadier.context.CommandContext;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ConfigValidator;
import net.minecraft.commands.CommandSourceStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Registry of config validators for the {@code /marie validate <modid>} command.
 * Each validator receives the command context and returns a brigadier result code.
 */
@ApiStatus.Internal
public final class ConfigValidatorRegistry {

    private static final Map<String, Function<CommandContext<CommandSourceStack>, Integer>> VALIDATORS =
            new LinkedHashMap<>();
    private static final Map<String, ConfigValidator> RAW_VALIDATORS = new LinkedHashMap<>();

    private ConfigValidatorRegistry() {}

    public static void register(String modId, Function<CommandContext<CommandSourceStack>, Integer> validator) {
        VALIDATORS.put(modId, validator);
    }

    public static void registerRaw(ConfigValidator validator) {
        RAW_VALIDATORS.put(validator.validatorId(), validator);
    }

    public static Set<String> getRegisteredModIds() {
        return Collections.unmodifiableSet(VALIDATORS.keySet());
    }

    public static Collection<ConfigValidator> getAllRaw() {
        return Collections.unmodifiableCollection(RAW_VALIDATORS.values());
    }

    public static @Nullable Function<CommandContext<CommandSourceStack>, Integer> get(String modId) {
        return VALIDATORS.get(modId);
    }
}
