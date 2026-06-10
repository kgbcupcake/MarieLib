package dev.marie.MariesLib.runtime;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.registry.AbstractRegistry;
import dev.marie.MariesLib.scanner.TokenStemmer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves source item IDs to logical families based on token matching against the path.
 * The path is tokenized the same way as scanner scoring ({@link TokenStemmer#rawSegmentsForPath}).
 * Each keyword must match a whole token (not a substring).
 * All resolutions are cached for O(1) repeated lookups.
 */
@ApiStatus.Internal
public final class FamilyResolver {

    private static final Map<ResourceLocation, String> CACHE = new ConcurrentHashMap<>();

    private static final class Core extends AbstractRegistry<String, String[]> {
        Core() {
            super("FamilyResolver");
        }
    }

    private static final Core INSTANCE = new Core();

    static {
        INSTANCE.reset();
        INSTANCE.freeze();
    }

    private FamilyResolver() {}

    /**
     * Resolves an item to its family, or null if no match.
     * Results are cached permanently for the session.
     */
    public static String resolve(ResourceLocation itemId) {
        if (itemId == null) return null;
        return CACHE.computeIfAbsent(itemId, FamilyResolver::doResolve);
    }

    private static String doResolve(ResourceLocation itemId) {
        Set<String> tokens = tokenize(itemId.getPath());
        for (String family : INSTANCE.keys()) {
            String[] keywords = INSTANCE.get(family);
            if (keywords == null) {
                continue;
            }
            for (String keyword : keywords) {
                if (tokens.contains(keyword)) return family;
            }
        }
        return null;
    }

    private static Set<String> tokenize(String path) {
        List<String> segs = TokenStemmer.rawSegmentsForPath(path);
        if (segs.size() > 64) return Set.of();
        Set<String> tokens = new HashSet<>(segs.size() * 2);
        for (String part : segs) {
            if (!part.isEmpty()) {
                tokens.add(part.toLowerCase(Locale.ROOT));
            }
        }
        return tokens;
    }

    /** Clears the resolution cache. Call on hot-reload only. */
    public static void clearCache() {
        CACHE.clear();
    }

    public static void replaceFamilies(Map<String, List<String>> configuredFamilies) {
        INSTANCE.runWrite(() -> {
            INSTANCE.resetUnlocked();
            for (Map.Entry<String, List<String>> entry : configuredFamilies.entrySet()) {
                INSTANCE.registerUnlocked(entry.getKey(), entry.getValue().toArray(String[]::new));
            }
            INSTANCE.freezeUnlocked();
        });
        clearCache();
    }
}
