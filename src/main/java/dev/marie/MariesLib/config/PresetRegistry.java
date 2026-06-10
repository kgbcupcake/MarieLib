package dev.marie.MariesLib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Loads and writes gameplay presets under {@code config/<modid>/presets/}.
 * Built-in files are copied from the jar on first run when missing.
 */
public final class PresetRegistry {

    public static final Set<String> BUILTIN_STEMS = Set.of("casual", "survival", "hardcore");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record PresetValues(
            double decayRate,
            double criticalThreshold,
            double lowThreshold,
            double excessThreshold,
            int defaultEffectDurationTicks,
            boolean enableDecay,
            boolean enableEffects
    ) {
        public static PresetValues fromJsonObject(JsonObject values) {
            double decay = values.has("decayRate") ? values.get("decayRate").getAsDouble() : 0.1d;
            double crit = values.has("criticalThreshold") ? values.get("criticalThreshold").getAsDouble() : 0.25d;
            double low = values.has("lowThreshold") ? values.get("lowThreshold").getAsDouble() : 0.4d;
            double excess = values.has("excessThreshold") ? values.get("excessThreshold").getAsDouble() : 0.9d;
            int dur = values.has("defaultEffectDurationTicks") ? values.get("defaultEffectDurationTicks").getAsInt() : 140;
            boolean decayOn = !values.has("enableDecay") || values.get("enableDecay").getAsBoolean();
            boolean effectsOn = !values.has("enableEffects") || values.get("enableEffects").getAsBoolean();
            return new PresetValues(decay, crit, low, excess, dur, decayOn, effectsOn);
        }

        public static PresetValues fromCurrentConfig() {
            throw new UnsupportedOperationException("Implement via consuming mod");
        }

        public static PresetValues empty() {
            return new PresetValues(0.1d, 0.25d, 0.4d, 0.9d, 140, false, false);
        }

        public JsonObject toJsonObject() {
            JsonObject o = new JsonObject();
            o.addProperty("decayRate", decayRate);
            o.addProperty("criticalThreshold", criticalThreshold);
            o.addProperty("lowThreshold", lowThreshold);
            o.addProperty("excessThreshold", excessThreshold);
            o.addProperty("defaultEffectDurationTicks", defaultEffectDurationTicks);
            o.addProperty("enableDecay", enableDecay);
            o.addProperty("enableEffects", enableEffects);
            return o;
        }
    }

    public record ParsedPreset(
            String fileStem,
            Path path,
            String name,
            String description,
            String author,
            boolean locked,
            boolean builtin,
            PresetValues values
    ) {
        public boolean canDelete() {
            return !builtin && !locked;
        }

        public boolean showLockIcon() {
            return locked || builtin;
        }
    }

    private PresetRegistry() {}

    public static Path presetsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(MarieLibContext.get().modId()).resolve("presets");
    }

    /**
     * Writes default Casual / Survival / Hardcore JSON files when they are absent.
     */
    public static void ensureBuiltInFilesOnDisk() {
        throw new UnsupportedOperationException("Implement via consuming mod");
    }

    public static void reload() {
        ensureBuiltInFilesOnDisk();
    }

    /**
     * Lists all {@code *.json} presets in the presets folder (newest files last after built-ins).
     */
    public static List<ParsedPreset> listPresets() {
        Path dir = presetsDirectory();
        List<ParsedPreset> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .forEach(p -> {
                        try {
                            ParsedPreset parsed = parseFile(p);
                            if (parsed != null) {
                                out.add(parsed);
                            }
                        } catch (Exception e) {
                            MariesLib.LOGGER.warn("[MarieLib] Skipping invalid preset {}", p, e);
                        }
                    });
        } catch (IOException e) {
            MariesLib.LOGGER.error("[MarieLib] Failed to list presets", e);
        }
        out.sort(Comparator
                .comparingInt((ParsedPreset p) -> builtinSortKey(p.fileStem()))
                .thenComparing(p -> p.name().toLowerCase(Locale.ROOT)));
        return out;
    }

    private static int builtinSortKey(String stem) {
        String s = stem.toLowerCase(Locale.ROOT);
        if ("casual".equals(s)) {
            return 0;
        }
        if ("survival".equals(s)) {
            return 1;
        }
        if ("hardcore".equals(s)) {
            return 2;
        }
        return 10;
    }

    public static ParsedPreset parseFile(Path file) throws IOException {
        if (Files.size(file) > 65536) {
            throw new IOException("preset file too large: " + file);
        }
        String stem = fileNameStem(file);
        try (Reader r = Files.newBufferedReader(file)) {
            return parseReader(stem, file, r);
        }
    }

    private static ParsedPreset parseReader(String fileStem, Path path, Reader reader) {
        JsonObject root = GSON.fromJson(reader, JsonObject.class);
        if (root == null) {
            return null;
        }
        String name = root.has("name") ? root.get("name").getAsString() : fileStem;
        String description = root.has("description") ? root.get("description").getAsString() : "";
        String author = root.has("author") ? root.get("author").getAsString() : "";
        boolean locked = root.has("locked") && root.get("locked").getAsBoolean();
        boolean builtin = BUILTIN_STEMS.contains(fileStem.toLowerCase(Locale.ROOT));
        JsonElement valuesEl = root.get("values");
        if (valuesEl == null || !valuesEl.isJsonObject()) {
            MariesLib.LOGGER.warn("[MarieLib] Preset {} has no values object", path);
            return null;
        }
        PresetValues values = PresetValues.fromJsonObject(valuesEl.getAsJsonObject());
        return new ParsedPreset(fileStem, path, name, description, author, locked, builtin, values);
    }

    private static String fileNameStem(Path file) {
        String fn = file.getFileName().toString();
        int dot = fn.lastIndexOf('.');
        return dot > 0 ? fn.substring(0, dot) : fn;
    }

    public static void applyPresetValues(PresetValues v) {
        throw new UnsupportedOperationException("Implement via consuming mod");
    }

    public static void applyPreset(ParsedPreset preset) {
        applyPresetValues(preset.values());
        if ("hardcore".equalsIgnoreCase(preset.fileStem())) {
            enableAllEffects();
        }
    }

    private static void enableAllEffects() {
        throw new UnsupportedOperationException("Implement via consuming mod");
    }

    public static void deletePreset(ParsedPreset preset) throws IOException {
        if (!preset.canDelete()) {
            throw new IOException("Cannot delete built-in or locked preset");
        }
        Files.deleteIfExists(preset.path());
    }

    /**
     * Writes a user preset. {@code displayName} becomes the JSON {@code name}; the file stem is derived and made unique.
     */
    public static Path saveUserPreset(String displayName, String description, String author, PresetValues values) throws IOException {
        Path dir = presetsDirectory();
        Files.createDirectories(dir);
        String baseStem = sanitizeFileStem(displayName);
        if (baseStem.isEmpty()) {
            baseStem = "preset";
        }
        String stem = uniquifyStem(dir, baseStem);
        Path file = dir.resolve(stem + ".json");
        JsonObject root = new JsonObject();
        root.addProperty("name", displayName.trim());
        root.addProperty("description", description == null ? "" : description.trim());
        root.addProperty("author", author == null ? "" : author.trim());
        root.addProperty("locked", false);
        root.add("values", values.toJsonObject());
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(root, w);
        }
        MariesLib.LOGGER.info("[MarieLib] Saved preset to {}", file);
        return file;
    }

    private static String uniquifyStem(Path dir, String base) throws IOException {
        String candidate = base;
        int n = 2;
        while (Files.exists(dir.resolve(candidate + ".json"))) {
            candidate = base + "_" + n;
            n++;
        }
        return candidate;
    }

    /**
     * Lowercase file stem: letters, digits, underscore only.
     */
    public static String sanitizeFileStem(String raw) {
        if (raw == null) {
            return "";
        }
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lower.length() && sb.length() < 60; i++) {
            char ch = lower.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_') {
                sb.append(ch);
            } else if (ch == ' ' || ch == '-' || ch == '.') {
                sb.append('_');
            }
        }
        String s = sb.toString().replaceAll("_+", "_");
        if (s.startsWith("_")) {
            s = s.substring(1);
        }
        if (s.endsWith("_")) {
            s = s.substring(0, s.length() - 1);
        }
        if (BUILTIN_STEMS.contains(s)) {
            return s + "_custom";
        }
        return s;
    }

}
