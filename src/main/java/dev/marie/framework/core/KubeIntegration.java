package dev.marie.MariesLib.core;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ValueModifierContext;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Optional KubeJS integration entry points. Uses reflection so MarieLib loads
 * without KubeJS on the runtime classpath.
 */
@ApiStatus.Internal
public final class KubeIntegration {

    private static final boolean PRESENT = ModList.get().isLoaded("kubejs");
    private static final String PIPELINE_HOOKS =
            "dev.marie.MariesLib.kubejs.internal.KubePipelineHooks";
    private static final String EVENT_BRIDGE =
            "dev.marie.MariesLib.kubejs.internal.KubeEventBridge";

    private KubeIntegration() {}

    public static void registerEventBridge() {
        if (!PRESENT) {
            return;
        }
        invokeStatic(EVENT_BRIDGE, "register");
    }

    public static float applyValueDeltaModifier(ValueModifierContext ctx, float delta) {
        if (!PRESENT) {
            return delta;
        }
        Object result = invokeStatic(
                PIPELINE_HOOKS,
                "applyValueDeltaModifier",
                new Class<?>[] {ValueModifierContext.class, float.class},
                ctx,
                delta
        );
        return result instanceof Float value ? value : delta;
    }

    public static float applyDecayTick(String playerId, String valueKey, float amount) {
        if (!PRESENT) {
            return amount;
        }
        Object result = invokeStatic(
                PIPELINE_HOOKS,
                "applyDecayTick",
                new Class<?>[] {String.class, String.class, float.class},
                playerId,
                valueKey,
                amount
        );
        return result instanceof Float value ? value : amount;
    }

    public static void firePlayerSynced(ServerPlayer player) {
        if (!PRESENT) {
            return;
        }
        invokeStatic(
                PIPELINE_HOOKS,
                "firePlayerSynced",
                new Class<?>[] {ServerPlayer.class},
                player
        );
    }

    private static void invokeStatic(String className, String method) {
        invokeStatic(className, method, new Class<?>[0]);
    }

    private static Object invokeStatic(String className, String method, Class<?>[] paramTypes, Object... args) {
        try {
            Class<?> type = Class.forName(className);
            return type.getMethod(method, paramTypes).invoke(null, args);
        } catch (ReflectiveOperationException e) {
            MariesLib.LOGGER.warn("[MarieLib] KubeJS integration call failed: {}.{}", className, method, e);
            return null;
        }
    }
}
