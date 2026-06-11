package dev.marie.MariesLib.api.registry;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ThresholdEffect;
import dev.marie.MariesLib.registry.AbstractRegistry;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Internal storage for threshold effects registered via the public API.
 */
@ApiStatus.Internal
public final class EffectRegistry {

    private static final class Core extends AbstractRegistry<String, ThresholdEffect> {
        Core() {
            super("EffectRegistry");
        }
    }

    private static final Core INSTANCE = new Core();
    private static final Set<String> LEGACY_CLEANUP_EFFECT_IDS = new LinkedHashSet<>();

    private EffectRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        INSTANCE.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        INSTANCE.reset();
        LEGACY_CLEANUP_EFFECT_IDS.clear();
    }

    public static void register(ThresholdEffect definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        String effectId = definition.getEffectId().toString();
        INSTANCE.register(effectId, definition);
    }

    @Nullable
    public static ThresholdEffect get(String effectId) {
        return INSTANCE.get(effectId);
    }

    public static List<ThresholdEffect> getAll() {
        return INSTANCE.values();
    }

    public static boolean isRegistered(String effectId) {
        return effectId != null && !effectId.isBlank() && INSTANCE.get(effectId) != null;
    }

    /**
     * Legacy Minecraft effect ids that should be stripped when not registered in {@link EffectRegistry}.
     */
    public static Set<String> legacyCleanupEffectIds() {
        return Set.copyOf(LEGACY_CLEANUP_EFFECT_IDS);
    }

    public static void registerLegacyCleanupEffectId(String effectId) {
        if (effectId != null && !effectId.isBlank()) {
            LEGACY_CLEANUP_EFFECT_IDS.add(effectId);
        }
    }
}
