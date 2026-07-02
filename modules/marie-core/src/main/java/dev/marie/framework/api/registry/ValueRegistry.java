package dev.marie.framework.api.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ValueDefinition;
import dev.marie.framework.registry.AbstractRegistry;

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

    public static boolean isFrozen() {
        return INSTANCE.isFrozen();
    }

    public static void register(ValueDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        INSTANCE.register(definition.getId(), definition);
    }

    @Nullable
    public static ValueDefinition get(String id) {
        return INSTANCE.get(id);
    }

    public static List<ValueDefinition> getAll() {
        return INSTANCE.values();
    }
}
