package dev.marie.MariesLib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import dev.marie.MariesLib.config.MariesLibConfigKeys;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.scanner.ScannerSpecRegistry;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves {@code config/marieslib.cfg} (JSON).
 */
public final class MariesLibConfigIO {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = MariesLib.MOD_ID + ".cfg";

    private MariesLibConfigIO() {}

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    public static void load() {
        MariesLibConfigHolder h = MariesLibConfigHolder.get();
        Path file = configPath();
        if (!Files.exists(file)) {
            h.loadScannerScalarsFromRegistry();
            save();
            MariesLib.LOGGER.info("[MariesLib] Wrote default {}", file);
            return;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root != null) {
                readIntoHolder(root, h);
            }
            MariesLib.LOGGER.info("[MariesLib] Loaded config from {}", file);
        } catch (IOException e) {
            MariesLib.LOGGER.error("[MariesLib] Failed to load {}, using defaults", file, e);
            h.loadScannerScalarsFromRegistry();
        }
    }

    public static void save() {
        MariesLibConfigHolder h = MariesLibConfigHolder.get();
        Path file = configPath();
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = holderToJson(h);
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(root, w);
            }
            patchScannerSpecScalars(h);
            ScannerSpecRegistry.reload();
            ModuleCache.refresh();
            MariesLib.LOGGER.info("[MariesLib] Saved config to {}", file);
        } catch (IOException e) {
            MariesLib.LOGGER.error("[MariesLib] Failed to save {}", file, e);
        }
    }

    static void readIntoHolder(JsonObject root, MariesLibConfigHolder h) {
        JsonObject modules = obj(root, sectionKey(MariesLibConfigKeys.ENABLE_DECAY));
        if (modules != null) {
            h.enableDecay = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_DECAY), h.enableDecay);
            h.enableSourceApplication = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_SOURCE_APPLICATION), h.enableSourceApplication);
            h.enableBlockHeavySources = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_BLOCK_HEAVY_SOURCES), h.enableBlockHeavySources);
            h.enableBlockLightSource = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_BLOCK_LIGHT_SOURCE), h.enableBlockLightSource);
            h.enableEffects = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_EFFECTS), h.enableEffects);
            h.enableHUD = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_HUD), h.enableHUD);
            h.enableToasts = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_TOASTS), h.enableToasts);
            h.enableSourceTooltips = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_SOURCE_TOOLTIPS), h.enableSourceTooltips);
            h.enableTotalTracking = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_TOTAL_TRACKING), h.enableTotalTracking);
            h.enableTrackingScreen = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_TRACKING_SCREEN), h.enableTrackingScreen);
            h.enableCriticalToasts = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_CRITICAL_TOASTS), h.enableCriticalToasts);
            h.enableSleepBonus = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_SLEEP_BONUS), h.enableSleepBonus);
            h.enableSynergies = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_SYNERGIES), h.enableSynergies);
            h.enableMilestones = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_MILESTONES), h.enableMilestones);
            h.enableSeasonHooks = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_SEASON_HOOKS), h.enableSeasonHooks);
            h.enableAbsorptionModifiers = bool(modules, leafKey(MariesLibConfigKeys.ENABLE_ABSORPTION_MODIFIERS), h.enableAbsorptionModifiers);
        }

        JsonObject debug = obj(root, sectionKey(MariesLibConfigKeys.ENABLE_DEBUG_LOGGING));
        if (debug != null) {
            h.enableDebugLogging = bool(debug, leafKey(MariesLibConfigKeys.ENABLE_DEBUG_LOGGING), h.enableDebugLogging);
        }

        JsonObject scanner = obj(root, sectionKey(MariesLibConfigKeys.SCANNER_CONFIDENCE_SPREAD_THRESHOLD));
        if (scanner != null) {
            h.scannerConfidenceSpreadThreshold = flt(scanner, leafKey(MariesLibConfigKeys.SCANNER_CONFIDENCE_SPREAD_THRESHOLD), h.scannerConfidenceSpreadThreshold);
            h.compositeRatioThreshold = flt(scanner, leafKey(MariesLibConfigKeys.COMPOSITE_RATIO_THRESHOLD), h.compositeRatioThreshold);
            h.scannerEnableRecipeInheritance = bool(scanner, leafKey(MariesLibConfigKeys.SCANNER_ENABLE_RECIPE_INHERITANCE), h.scannerEnableRecipeInheritance);
            h.multiValueInheritanceThreshold = dbl(scanner, leafKey(MariesLibConfigKeys.MULTI_VALUE_INHERITANCE_THRESHOLD), h.multiValueInheritanceThreshold);
            JsonObject mult = obj(scanner, "multipliers");
            if (mult != null) {
                h.multCommunityTag = flt(mult, "communityTag", h.multCommunityTag);
                h.multNamespace = flt(mult, "namespace", h.multNamespace);
                h.multSuffix = flt(mult, "suffix", h.multSuffix);
                h.multKeyword = flt(mult, "keyword", h.multKeyword);
                h.multArchetype = flt(mult, "archetype", h.multArchetype);
                h.multRecipeInheritance = flt(mult, "recipeInheritance", h.multRecipeInheritance);
                h.multNamespacePeer = flt(mult, "namespacePeer", h.multNamespacePeer);
                h.multSecondarySuffix = flt(mult, "secondarySuffix", h.multSecondarySuffix);
                h.multNamespacePeerAverageWeight = flt(mult, "namespacePeerAverageWeight", h.multNamespacePeerAverageWeight);
            }
        }

        JsonObject memory = obj(root, "memory");
        if (memory != null) {
            h.memoryWindowMinutes = lng(memory, "memoryWindowMinutes", h.memoryWindowMinutes);
            h.memoryWindowCount = intVal(memory, "memoryWindowCount", h.memoryWindowCount);
            h.streakWindowMs = lng(memory, "streakWindowMs", h.streakWindowMs);
            h.streakWeight = flt(memory, "streakWeight", h.streakWeight);
            h.debtThreshold = flt(memory, "debtThreshold", h.debtThreshold);
            h.debtDecayRate = flt(memory, "debtDecayRate", h.debtDecayRate);
            h.diminishingSteepness = flt(memory, "diminishingSteepness", h.diminishingSteepness);
            h.diminishingMidpoint = flt(memory, "diminishingMidpoint", h.diminishingMidpoint);
            h.noveltyBonus = dbl(memory, "noveltyBonus", h.noveltyBonus);
            h.noveltyDecayCap = dbl(memory, "noveltyDecayCap", h.noveltyDecayCap);
            h.diminishingFloor = dbl(memory, "diminishingFloor", h.diminishingFloor);
            h.startingValueFill = dbl(memory, "startingValueFill", h.startingValueFill);
            h.debugMemoryLogging = bool(memory, "debugMemoryLogging", h.debugMemoryLogging);
        }

        JsonObject thresholds = obj(root, "thresholds");
        if (thresholds != null) {
            h.excessThreshold = flt(thresholds, "excessThreshold", h.excessThreshold);
            h.lowThreshold = flt(thresholds, "lowThreshold", h.lowThreshold);
            h.criticalThreshold = flt(thresholds, "criticalThreshold", h.criticalThreshold);
            h.decayIntervalTicks = intVal(thresholds, "decayIntervalTicks", h.decayIntervalTicks);
            h.defaultDecayRate = flt(thresholds, "defaultDecayRate", h.defaultDecayRate);
        }

        JsonObject effects = obj(root, "effects");
        if (effects != null) {
            h.defaultEffectDurationTicks = intVal(effects, "defaultEffectDurationTicks", h.defaultEffectDurationTicks);
        }

        JsonObject client = obj(root, "client");
        if (client != null) {
            h.showJoinMessage = bool(client, "showJoinMessage", h.showJoinMessage);
        }
    }

    static JsonObject holderToJson(MariesLibConfigHolder h) {
        JsonObject root = new JsonObject();

        JsonObject modules = new JsonObject();
        modules.addProperty("enableDecay", h.enableDecay);
        modules.addProperty("enableSourceApplication", h.enableSourceApplication);
        modules.addProperty("enableBlockHeavySources", h.enableBlockHeavySources);
        modules.addProperty("enableBlockLightSource", h.enableBlockLightSource);
        modules.addProperty("enableEffects", h.enableEffects);
        modules.addProperty("enableHUD", h.enableHUD);
        modules.addProperty("enableToasts", h.enableToasts);
        modules.addProperty("enableSourceTooltips", h.enableSourceTooltips);
        modules.addProperty("enableTotalTracking", h.enableTotalTracking);
        modules.addProperty("enableTrackingScreen", h.enableTrackingScreen);
        modules.addProperty("enableCriticalToasts", h.enableCriticalToasts);
        modules.addProperty("enableSleepBonus", h.enableSleepBonus);
        modules.addProperty("enableSynergies", h.enableSynergies);
        modules.addProperty("enableMilestones", h.enableMilestones);
        modules.addProperty("enableSeasonHooks", h.enableSeasonHooks);
        modules.addProperty("enableAbsorptionModifiers", h.enableAbsorptionModifiers);
        root.add("modules", modules);

        JsonObject debug = new JsonObject();
        debug.addProperty("enableDebugLogging", h.enableDebugLogging);
        root.add("debug", debug);

        JsonObject scanner = new JsonObject();
        scanner.addProperty("confidenceSpreadThreshold", h.scannerConfidenceSpreadThreshold);
        scanner.addProperty("compositeRatioThreshold", h.compositeRatioThreshold);
        scanner.addProperty("enableRecipeInheritance", h.scannerEnableRecipeInheritance);
        scanner.addProperty("multiValueInheritanceThreshold", h.multiValueInheritanceThreshold);
        JsonObject mult = new JsonObject();
        mult.addProperty("communityTag", h.multCommunityTag);
        mult.addProperty("namespace", h.multNamespace);
        mult.addProperty("suffix", h.multSuffix);
        mult.addProperty("keyword", h.multKeyword);
        mult.addProperty("archetype", h.multArchetype);
        mult.addProperty("recipeInheritance", h.multRecipeInheritance);
        mult.addProperty("namespacePeer", h.multNamespacePeer);
        mult.addProperty("secondarySuffix", h.multSecondarySuffix);
        mult.addProperty("namespacePeerAverageWeight", h.multNamespacePeerAverageWeight);
        scanner.add("multipliers", mult);
        root.add("scanner", scanner);

        JsonObject memory = new JsonObject();
        memory.addProperty("memoryWindowMinutes", h.memoryWindowMinutes);
        memory.addProperty("memoryWindowCount", h.memoryWindowCount);
        memory.addProperty("streakWindowMs", h.streakWindowMs);
        memory.addProperty("streakWeight", h.streakWeight);
        memory.addProperty("debtThreshold", h.debtThreshold);
        memory.addProperty("debtDecayRate", h.debtDecayRate);
        memory.addProperty("diminishingSteepness", h.diminishingSteepness);
        memory.addProperty("diminishingMidpoint", h.diminishingMidpoint);
        memory.addProperty("noveltyBonus", h.noveltyBonus);
        memory.addProperty("noveltyDecayCap", h.noveltyDecayCap);
        memory.addProperty("diminishingFloor", h.diminishingFloor);
        memory.addProperty("startingValueFill", h.startingValueFill);
        memory.addProperty("debugMemoryLogging", h.debugMemoryLogging);
        root.add("memory", memory);

        JsonObject thresholds = new JsonObject();
        thresholds.addProperty("excessThreshold", h.excessThreshold);
        thresholds.addProperty("lowThreshold", h.lowThreshold);
        thresholds.addProperty("criticalThreshold", h.criticalThreshold);
        thresholds.addProperty("decayIntervalTicks", h.decayIntervalTicks);
        thresholds.addProperty("defaultDecayRate", h.defaultDecayRate);
        root.add("thresholds", thresholds);

        JsonObject effects = new JsonObject();
        effects.addProperty("defaultEffectDurationTicks", h.defaultEffectDurationTicks);
        root.add("effects", effects);

        JsonObject client = new JsonObject();
        client.addProperty("showJoinMessage", h.showJoinMessage);
        root.add("client", client);

        return root;
    }

    static void patchScannerSpecScalars(MariesLibConfigHolder h) throws IOException {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(MariesLib.MOD_ID);
        Path file = configDir.resolve("scanner_spec.json");
        Files.createDirectories(configDir);
        JsonObject root;
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                root = GSON.fromJson(r, JsonObject.class);
            }
            if (root == null) {
                root = new JsonObject();
            }
        } else {
            root = new JsonObject();
        }

        JsonObject mult = root.has("multipliers") && root.get("multipliers").isJsonObject()
                ? root.getAsJsonObject("multipliers")
                : new JsonObject();
        mult.addProperty("community_tag", h.multCommunityTag);
        mult.addProperty("namespace", h.multNamespace);
        mult.addProperty("suffix", h.multSuffix);
        mult.addProperty("keyword", h.multKeyword);
        mult.addProperty("archetype", h.multArchetype);
        mult.addProperty("recipe_inheritance", h.multRecipeInheritance);
        mult.addProperty("namespace_peer", h.multNamespacePeer);
        mult.addProperty("secondary_suffix", h.multSecondarySuffix);
        mult.addProperty("namespace_peer_average_weight", h.multNamespacePeerAverageWeight);
        root.add("multipliers", mult);

        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(root, w);
        }
    }

    private static JsonObject obj(JsonObject parent, String key) {
        if (parent.has(key) && parent.get(key).isJsonObject()) {
            return parent.getAsJsonObject(key);
        }
        return null;
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        return o.has(key) ? o.get(key).getAsBoolean() : fallback;
    }

    private static int intVal(JsonObject o, String key, int fallback) {
        return o.has(key) ? o.get(key).getAsInt() : fallback;
    }

    private static long lng(JsonObject o, String key, long fallback) {
        return o.has(key) ? o.get(key).getAsLong() : fallback;
    }

    private static float flt(JsonObject o, String key, float fallback) {
        return o.has(key) ? o.get(key).getAsFloat() : fallback;
    }

    private static double dbl(JsonObject o, String key, double fallback) {
        return o.has(key) ? o.get(key).getAsDouble() : fallback;
    }

    private static String sectionKey(String dottedKey) {
        int dot = dottedKey.indexOf('.');
        return dot >= 0 ? dottedKey.substring(0, dot) : dottedKey;
    }

    private static String leafKey(String dottedKey) {
        int dot = dottedKey.lastIndexOf('.');
        return dot >= 0 ? dottedKey.substring(dot + 1) : dottedKey;
    }
}
