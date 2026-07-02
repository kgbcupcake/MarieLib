package dev.marie.framework.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MariesLib;
import dev.marie.framework.core.MarieContext;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data-driven mod compatibility engine for NeoForge 1.21.1.
 * <p>
 * Three-tier priority system:
 * <ol>
 *   <li>MarieLib built-in (data/&lt;modid&gt;/compat/compat_registry.json)</li>
 *   <li>Mod-provided (data/&lt;modid&gt;/marie_compat.json)</li>
 *   <li>Modpack override (config/&lt;modid&gt;/compat_overrides.json)</li>
 * </ol>
 */
@ApiStatus.Internal
public final class ModCompat {

    private static final Logger LOGGER = MariesLib.LOGGER;
    private static final Gson GSON = new GsonBuilder().create();

    private static volatile boolean initialized = false;

    private static final Map<String, CompatEntry> ENTRIES_BY_MODID = new LinkedHashMap<>();
    private static final Map<String, String> NAMESPACE_TO_MODID = new HashMap<>();
    private static final Set<String> BUILT_IN_MODIDS = new LinkedHashSet<>();

    public static boolean ANY_EFFECTS_CONFLICT = false;
    public static boolean ANY_SURVIVAL_OVERHAUL_LOADED = false;
    public static boolean ANY_DECAY_CONFLICT = false;
    public static ConflictBehavior MERGED_CONFLICT_BEHAVIOR = ConflictBehavior.NONE;

    private ModCompat() {}

    /**
     * Initialize the compat registry. Call once during mod initialization.
     * Loads all three tiers and resolves runtime data.
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;

        LOGGER.info("[MarieLib] Initializing mod compatibility registry...");

        Map<String, JsonCompatEntry> merged = new LinkedHashMap<>();

        loadTier1BuiltIn(merged);
        loadTier2ModProvided(merged);
        loadTier3ModpackOverride(merged);

        resolveRuntimeData(merged);
        buildNamespaceMap();
        computeAggregateFlags();
        logCompatReport();
    }

    /**
     * Call after item registry is available to discover unknown source mods.
     * Safe to call multiple times; only runs discovery once.
     */
    public static void discoverUnknownMods() {
        if (!initialized) {
            LOGGER.warn("[MarieLib] ModCompat.discoverUnknownMods() called before initialize()");
            return;
        }
        // Compat discovery is now strictly registry-driven.
    }

    private static InputStream openClasspathResource(String resourcePath) {
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            InputStream fromContext = contextLoader.getResourceAsStream(normalized);
            if (fromContext != null) {
                return fromContext;
            }
        }
        return ModCompat.class.getResourceAsStream("/" + normalized);
    }

    private static void loadTier1BuiltIn(Map<String, JsonCompatEntry> merged) {
        String resourcePath = "data/" + MarieContext.get().modId() + "/compat/compat_registry.json";
        try (InputStream is = openClasspathResource(resourcePath)) {
            if (is == null) {
                LOGGER.warn("[MarieLib] Built-in compat_registry.json not found in jar");
                return;
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                CompatRegistry registry = GSON.fromJson(reader, CompatRegistry.class);
                if (registry != null && registry.entries() != null) {
                    for (JsonCompatEntry entry : registry.entries()) {
                        if (entry.modId() != null) {
                            merged.put(entry.modId(), entry);
                            BUILT_IN_MODIDS.add(entry.modId());
                        }
                    }
                    LOGGER.debug("[MarieLib] Tier 1 (built-in): loaded {} entries", registry.entries().size());
                }
            }
        } catch (IOException e) {
            LOGGER.error("[MarieLib] Failed to load built-in compat_registry.json", e);
        }
    }

    private static void loadTier2ModProvided(Map<String, JsonCompatEntry> merged) {
        int count = 0;
        for (IModInfo modInfo : ModList.get().getMods()) {
            String modId = modInfo.getModId();
            if ("minecraft".equals(modId) || MarieContext.get().modId().equals(modId) || "neoforge".equals(modId)) {
                continue;
            }

            String resourcePath = "data/" + modId + "/marie_compat.json";
            try (InputStream is = openClasspathResource(resourcePath)) {
                if (is != null) {
                    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        CompatRegistry registry = GSON.fromJson(reader, CompatRegistry.class);
                        if (registry != null && registry.entries() != null) {
                            for (JsonCompatEntry entry : registry.entries()) {
                                if (entry.modId() != null) {
                                    mergeEntry(merged, entry);
                                    count++;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("[ModCompat] Failed to read compat file from mod {}: {}", modId, e.getMessage());
                LOGGER.debug("[MarieLib] No compat file from mod {}", modId);
            }
        }
        if (count > 0) {
            LOGGER.debug("[MarieLib] Tier 2 (mod-provided): merged {} entries", count);
        }
    }

    private static void loadTier3ModpackOverride(Map<String, JsonCompatEntry> merged) {
        Path overridePath = FMLPaths.CONFIGDIR.get().resolve(MarieContext.get().modId()).resolve("compat_overrides.json");
        if (!Files.exists(overridePath)) {
            LOGGER.debug("[MarieLib] Tier 3 (modpack override): no compat_overrides.json found");
            return;
        }

        try (Reader reader = Files.newBufferedReader(overridePath, StandardCharsets.UTF_8)) {
            CompatRegistry registry = GSON.fromJson(reader, CompatRegistry.class);
            if (registry != null && registry.entries() != null) {
                for (JsonCompatEntry entry : registry.entries()) {
                    if (entry.modId() != null) {
                        mergeEntry(merged, entry);
                    }
                }
                LOGGER.debug("[MarieLib] Tier 3 (modpack override): merged {} entries", registry.entries().size());
            }
        } catch (IOException e) {
            LOGGER.error("[MarieLib] Failed to load compat_overrides.json", e);
        }
    }

    private static void mergeEntry(Map<String, JsonCompatEntry> merged, JsonCompatEntry newEntry) {
        JsonCompatEntry existing = merged.get(newEntry.modId());
        if (existing == null) {
            merged.put(newEntry.modId(), newEntry);
            return;
        }

        List<String> mergedNamespaces = new ArrayList<>(existing.namespaces() != null ? existing.namespaces() : List.of());
        if (newEntry.namespaces() != null) {
            for (String ns : newEntry.namespaces()) {
                if (!mergedNamespaces.contains(ns)) {
                    mergedNamespaces.add(ns);
                }
            }
        }

        JsonCompatEntry mergedEntry = new JsonCompatEntry(
                newEntry.modId(),
                newEntry.displayName() != null ? newEntry.displayName() : existing.displayName(),
                newEntry.category() != null ? newEntry.category() : existing.category(),
                mergedNamespaces,
                newEntry.providesSourceTags() || existing.providesSourceTags(),
                newEntry.handlesOwnValues() || existing.handlesOwnValues(),
                newEntry.versionRanges() != null && !newEntry.versionRanges().isEmpty()
                        ? newEntry.versionRanges() : existing.versionRanges(),
                newEntry.conflictBehavior() != null ? newEntry.conflictBehavior() : existing.conflictBehavior(),
                newEntry.softCompat() || existing.softCompat(),
                newEntry.priority() > 0 ? newEntry.priority() : existing.priority()
        );

        merged.put(newEntry.modId(), mergedEntry);
    }

    private static void resolveRuntimeData(Map<String, JsonCompatEntry> merged) {
        for (Map.Entry<String, JsonCompatEntry> entry : merged.entrySet()) {
            String modId = entry.getKey();
            JsonCompatEntry json = entry.getValue();

            boolean loaded = ModList.get().isLoaded(modId);
            String detectedVersion = null;
            ConflictLevel resolvedLevel = ConflictLevel.NONE;

            if (loaded) {
                Optional<? extends IModInfo> modInfo = ModList.get().getMods().stream()
                        .filter(m -> m.getModId().equals(modId))
                        .findFirst();

                if (modInfo.isPresent()) {
                    ArtifactVersion version = modInfo.get().getVersion();
                    detectedVersion = version.toString();
                    resolvedLevel = resolveVersionConflict(detectedVersion, json.versionRanges());
                }
            }

            CompatEntry resolved = json.toCompatEntry(loaded, detectedVersion, resolvedLevel);
            if (ENTRIES_BY_MODID.containsKey(modId)) {
                LOGGER.info("[MarieLib] Skipping duplicate compat entry for: {}", modId);
                continue;
            }
            ENTRIES_BY_MODID.put(modId, resolved);
        }
    }

    private static ConflictLevel resolveVersionConflict(@Nullable String detectedVersion, @Nullable Map<String, ConflictLevel> versionRanges) {
        if (versionRanges == null || versionRanges.isEmpty()) {
            return ConflictLevel.NONE;
        }

        if (detectedVersion == null) {
            return ConflictLevel.PARTIAL_CONFLICT;
        }

        SemVer detected = SemVer.parse(detectedVersion);
        if (detected == null) {
            LOGGER.warn("[MarieLib] Could not parse mod version '{}' as semver, assuming partial conflict", detectedVersion);
            return ConflictLevel.PARTIAL_CONFLICT;
        }

        for (Map.Entry<String, ConflictLevel> range : versionRanges.entrySet()) {
            if (detected.satisfies(range.getKey())) {
                return range.getValue();
            }
        }

        return ConflictLevel.NONE;
    }

    private static void buildNamespaceMap() {
        NAMESPACE_TO_MODID.clear();
        for (CompatEntry entry : ENTRIES_BY_MODID.values()) {
            for (String namespace : entry.namespaces()) {
                NAMESPACE_TO_MODID.put(namespace, entry.modId());
            }
        }
    }

    private static void computeAggregateFlags() {
        ConflictBehavior merged = ConflictBehavior.NONE;
        boolean anyEffects = false;
        boolean anySurvival = false;
        boolean anyDecay = false;

        for (CompatEntry entry : ENTRIES_BY_MODID.values()) {
            if (!entry.loaded()) continue;

            if (entry.category() == CompatCategory.SURVIVAL_OVERHAUL) {
                anySurvival = true;
            }

            if (entry.conflictBehavior() != null &&
                    entry.resolvedConflictLevel() != ConflictLevel.NONE) {
                merged = merged.merge(entry.conflictBehavior());

                if (entry.conflictBehavior().disableEffects()) {
                    anyEffects = true;
                }
                if (entry.conflictBehavior().disableDecay()) {
                    anyDecay = true;
                }
            }
        }

        ANY_EFFECTS_CONFLICT = anyEffects;
        ANY_SURVIVAL_OVERHAUL_LOADED = anySurvival;
        ANY_DECAY_CONFLICT = anyDecay;
        MERGED_CONFLICT_BEHAVIOR = merged;
    }

    private static void logCompatReport() {
        List<CompatEntry> survivalOverhauls = getLoadedByCategory(CompatCategory.SURVIVAL_OVERHAUL);
        List<CompatEntry> sourceMods = getLoadedByCategory(CompatCategory.SOURCE_MOD);
        List<CompatEntry> farmingMods = getLoadedByCategory(CompatCategory.FARMING_MOD);

        LOGGER.info("[MarieLib] Compat Registry loaded — 3 tiers merged");

        if (!survivalOverhauls.isEmpty()) {
            String survivalList = survivalOverhauls.stream()
                    .map(e -> {
                        String name = e.modId();
                        if (e.detectedVersion() != null) {
                            name += " (v" + e.detectedVersion();
                            if (e.conflictBehavior() != null && e.conflictBehavior().disableEffects()) {
                                name += " — effects disabled";
                            }
                            name += ")";
                        }
                        return name;
                    })
                    .collect(Collectors.joining(", "));
            LOGGER.info("[MarieLib] Survival overhauls detected: {}", survivalList);
        }

        if (!sourceMods.isEmpty()) {
            String sourceList = sourceMods.stream()
                    .map(CompatEntry::modId)
                    .collect(Collectors.joining(", "));
            LOGGER.info("[MarieLib] Source mods detected: {} ({} mods)", sourceList, sourceMods.size());
        }

        if (!farmingMods.isEmpty()) {
            String farmList = farmingMods.stream()
                    .map(CompatEntry::modId)
                    .collect(Collectors.joining(", "));
            LOGGER.info("[MarieLib] Farming mods detected: {} ({} mods)", farmList, farmingMods.size());
        }

        LOGGER.info("[MarieLib] Merged conflict behavior: disableEffects={}, disableDecay={}",
                MERGED_CONFLICT_BEHAVIOR.disableEffects(),
                MERGED_CONFLICT_BEHAVIOR.disableDecay());
    }

    /**
     * Registers an externally-defined compatibility entry via the public API.
     * Called by the consuming mod's API to register a compat entry.
     */
    public static void registerExternal(CompatDefinition definition) {
        CompatCategory category = switch (definition.getCategory()) {
            case SOURCE_MOD -> CompatCategory.SOURCE_MOD;
            case FARMING_MOD -> CompatCategory.FARMING_MOD;
            case SURVIVAL_OVERHAUL -> CompatCategory.SURVIVAL_OVERHAUL;
        };
        boolean loaded = net.neoforged.fml.ModList.get().isLoaded(definition.getModId());
        CompatEntry entry = new CompatEntry(
                definition.getModId(),
                definition.getModId(),
                category,
                List.of(definition.getModId()),
                false,
                false,
                Map.of(),
                null,
                false,
                0,
                loaded,
                null,
                ConflictLevel.NONE
        );
        if (ENTRIES_BY_MODID.containsKey(definition.getModId())) {
            LOGGER.info("[MarieLib] Skipping duplicate compat entry for: {}", definition.getModId());
            return;
        }
        ENTRIES_BY_MODID.put(definition.getModId(), entry);
        NAMESPACE_TO_MODID.put(definition.getModId(), definition.getModId());
        LOGGER.info("[MarieLib] Registered external compat entry: {}", definition.getModId());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Public Query API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Returns a map of all registered mod IDs to their loaded status.
     * Derived dynamically from the registry — always in sync.
     */
    public static Map<String, Boolean> getDetected() {
        return ENTRIES_BY_MODID.values().stream()
                .collect(Collectors.toMap(CompatEntry::modId, CompatEntry::loaded));
    }

    /**
     * Maps an item namespace to the modid key used in compat config.
     * Returns the namespace as-is if it's a tracked mod, null if not tracked.
     */
    public static @Nullable String namespaceToModid(String namespace) {
        return NAMESPACE_TO_MODID.get(namespace);
    }

    /**
     * Get the full compat entry for a namespace if it exists.
     */
    public static Optional<CompatEntry> entryForNamespace(String namespace) {
        String modId = NAMESPACE_TO_MODID.get(namespace);
        return modId != null ? Optional.ofNullable(ENTRIES_BY_MODID.get(modId)) : Optional.empty();
    }

    /**
     * Get the full compat entry for a mod if it exists in the registry.
     */
    public static Optional<CompatEntry> getEntry(String modId) {
        return Optional.ofNullable(ENTRIES_BY_MODID.get(modId));
    }

    /**
     * Is this mod loaded?
     */
    public static boolean isLoaded(String modId) {
        CompatEntry entry = ENTRIES_BY_MODID.get(modId);
        return entry != null && entry.loaded();
    }

    /**
     * Is this mod loaded AND has a conflict at or above the given level?
     */
    public static boolean hasConflict(String modId, ConflictLevel minimum) {
        CompatEntry entry = ENTRIES_BY_MODID.get(modId);
        if (entry == null || !entry.loaded()) return false;
        return entry.resolvedConflictLevel().ordinal() >= minimum.ordinal();
    }

    /**
     * Get all loaded entries by category.
     */
    public static List<CompatEntry> getLoadedByCategory(CompatCategory category) {
        return ENTRIES_BY_MODID.values().stream()
                .filter(e -> e.loaded() && e.category() == category)
                .toList();
    }

    /**
     * Get all loaded entries that provide source tags.
     */
    public static List<CompatEntry> getSourceTagProviders() {
        return ENTRIES_BY_MODID.values().stream()
                .filter(e -> e.loaded() && e.providesSourceTags())
                .toList();
    }

    /**
     * Should the consuming mod disable effects given current loaded mods?
     */
    public static boolean shouldDisableEffects() {
        return MERGED_CONFLICT_BEHAVIOR.disableEffects();
    }

    /**
     * Should the consuming mod disable decay given current loaded mods?
     */
    public static boolean shouldDisableDecay() {
        return MERGED_CONFLICT_BEHAVIOR.disableDecay();
    }

    /**
     * Full compat report for config screen display.
     */
    public static List<CompatReportEntry> getCompatReport() {
        return ENTRIES_BY_MODID.values().stream()
                .sorted(Comparator
                        .comparing((CompatEntry e) -> !e.loaded())
                        .thenComparing(e -> e.category().ordinal())
                        .thenComparing(CompatEntry::modId))
                .map(e -> CompatReportEntry.from(e, MERGED_CONFLICT_BEHAVIOR))
                .toList();
    }

    /**
     * Get all registered entries (for debugging/inspection).
     */
    public static Collection<CompatEntry> getAllEntries() {
        return Collections.unmodifiableCollection(ENTRIES_BY_MODID.values());
    }

    /**
     * Built-in tier 1 entries shipped directly by the consuming mod (compat_registry.json).
     */
    public static List<CompatEntry> getBuiltInEntries() {
        return BUILT_IN_MODIDS.stream()
                .map(ENTRIES_BY_MODID::get)
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((CompatEntry e) -> !e.loaded())
                        .thenComparing(e -> e.category().ordinal())
                        .thenComparing(CompatEntry::modId))
                .toList();
    }
}
