package dev.marie.framework.api.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.SleepBonusEvaluator;
import dev.marie.framework.registry.ListRegistry;

import java.util.List;

@ApiStatus.Internal
public final class SleepBonusEvaluatorRegistry {

    private static final ListRegistry<SleepBonusEvaluator> REGISTRY =
            new ListRegistry<>("SleepBonusEvaluatorRegistry", null);

    private SleepBonusEvaluatorRegistry() {}

    public static void register(SleepBonusEvaluator evaluator) {
        REGISTRY.register(evaluator);
    }

    public static List<SleepBonusEvaluator> getAll() {
        return REGISTRY.values();
    }

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        REGISTRY.reset();
    }
}
