package dev.marie.MariesLib.runtime;

import com.mojang.logging.LogUtils;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.scanner.ItemScanner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Framework-level registry for external and scanner-applied source classifications.
 */
@ApiStatus.Internal
public class SourceRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** @GuardedBy("itself — ConcurrentHashSet") */
    private static final Set<String> WARNED_ITEMS = ConcurrentHashMap.newKeySet();

    /** @GuardedBy("itself — ConcurrentHashSet") Per-reload dedupe for empty blend warnings. */
    private static final Set<String> EMPTY_BLEND_WARNED = ConcurrentHashMap.newKeySet();

    /** @GuardedBy("itself — ConcurrentHashSet") Per-session dedupe for external classification cap warnings. */
    private static final Set<String> WARNED_CAP_ITEMS = ConcurrentHashMap.newKeySet();

    private static final int EXTERNAL_CLASSIFICATION_CAP = 4096;

    /** @GuardedBy("itself — ConcurrentHashMap") */
    private static final Map<ResourceLocation, Map<String, Float>> EXTERNAL_CLASSIFICATIONS = new ConcurrentHashMap<>();

    /** @GuardedBy("itself — ConcurrentHashMap") API/KubeJS registrations that must survive reload clears. */
    private static final Map<ResourceLocation, Map<String, Float>> API_REGISTERED_CLASSIFICATIONS = new ConcurrentHashMap<>();

    /** @GuardedBy("itself — ConcurrentHashMap") */
    private static final Map<ResourceLocation, Map<String, Float>> SCANNER_CLASSIFICATIONS = new ConcurrentHashMap<>();

    private SourceRegistry() {}

    public static void registerClassification(ResourceLocation sourceId, String valueKey, float amount) {
        if (EXTERNAL_CLASSIFICATIONS.size() >= EXTERNAL_CLASSIFICATION_CAP && !EXTERNAL_CLASSIFICATIONS.containsKey(sourceId)) {
            if (WARNED_CAP_ITEMS.add(sourceId.toString())) {
                LOGGER.warn("[SourceRegistry] External classification cap ({}) reached — ignoring: {} -> {}",
                        EXTERNAL_CLASSIFICATION_CAP, sourceId, valueKey);
            }
            return;
        }
        EXTERNAL_CLASSIFICATIONS.computeIfAbsent(sourceId, k -> new ConcurrentHashMap<>()).put(valueKey, amount);
        API_REGISTERED_CLASSIFICATIONS.computeIfAbsent(sourceId, k -> new ConcurrentHashMap<>()).put(valueKey, amount);
        LOGGER.debug("[SourceRegistry] Registered external classification: {} -> {} ({})", sourceId, valueKey, amount);
    }

    public static void clearExternalClassifications() {
        EXTERNAL_CLASSIFICATIONS.clear();
        for (Map.Entry<ResourceLocation, Map<String, Float>> entry : API_REGISTERED_CLASSIFICATIONS.entrySet()) {
            EXTERNAL_CLASSIFICATIONS.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
        }
    }

    public static void applyFromScanner(Map<ResourceLocation, Map<String, Float>> perItemValues) {
        SCANNER_CLASSIFICATIONS.clear();
        for (Map.Entry<ResourceLocation, Map<String, Float>> entry : perItemValues.entrySet()) {
            ResourceLocation itemId = entry.getKey();
            if (EXTERNAL_CLASSIFICATIONS.containsKey(itemId)) {
                continue;
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            if (ItemScanner.hasValueTag(stack)) {
                continue;
            }
            Map<String, Float> inner = entry.getValue();
            if (inner == null || inner.isEmpty()) {
                continue;
            }
            SCANNER_CLASSIFICATIONS.put(itemId, new ConcurrentHashMap<>(inner));
        }
        RuntimeResolver.getInstance().invalidateCache();
        LOGGER.info("[SourceRegistry] Scanner applied {} classifications", SCANNER_CLASSIFICATIONS.size());
    }

    public static void clearScannerClassifications() {
        SCANNER_CLASSIFICATIONS.clear();
    }

    public static void clearPerReloadWarnings() {
        WARNED_ITEMS.clear();
        EMPTY_BLEND_WARNED.clear();
    }

    public static void clearSessionWarnings() {
        WARNED_CAP_ITEMS.clear();
    }

    public static Map<String, Float> getExternalClassification(ResourceLocation sourceId) {
        Map<String, Float> api = EXTERNAL_CLASSIFICATIONS.get(sourceId);
        if (api != null) {
            return api;
        }
        return SCANNER_CLASSIFICATIONS.get(sourceId);
    }

    public static boolean hasAuthoritativeClassification(ResourceLocation sourceId) {
        return EXTERNAL_CLASSIFICATIONS.containsKey(sourceId);
    }

    static boolean hasApiClassification(ResourceLocation sourceId) {
        return EXTERNAL_CLASSIFICATIONS.containsKey(sourceId);
    }

    static boolean hasScannerClassification(ResourceLocation sourceId) {
        return SCANNER_CLASSIFICATIONS.containsKey(sourceId);
    }
}
