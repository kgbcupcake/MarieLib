package dev.marie.MariesLib.api.registry;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.registry.AbstractRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Internal storage for value definitions registered via the public API.
 */
@ApiStatus.Internal
public final class ValueRegistry {

    private static final class Core extends AbstractRegistry<String, ValueDefinition> {
        Core() {
            super("ValueRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private ValueRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        INSTANCE.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        INSTANCE.reset();
    }

    @ApiStatus.Stable
    public static void register(ValueDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        INSTANCE.register(definition.getId(), definition);
    }

    @Nullable
    @ApiStatus.Stable
    public static ValueDefinition get(String id) {
        return INSTANCE.get(id);
    }

    @ApiStatus.Stable
    public static List<ValueDefinition> getAll() {
        return INSTANCE.values();
    }
}
