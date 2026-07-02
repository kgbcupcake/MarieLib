package dev.marie.framework.command;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MariesLib;
import dev.marie.framework.data.DatapackDiagnostic;
import dev.marie.framework.data.DatapackDiagnostics;
import dev.marie.framework.data.SchemaDefinition;
import dev.marie.framework.datapack.SchemaTemplateGenerator;
import dev.marie.framework.handler.ReloadGuardListener;
import dev.marie.framework.handler.ReloadPipeline;
import dev.marie.framework.util.MarieValidation;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApiStatus.Internal
final class MarieDatapackCommands {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_DIAGNOSTICS_LINES = 100;

    private MarieDatapackCommands() {}

    static int reloadAll(CommandContext<CommandSourceStack> ctx, String modId) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> Component.literal("Reloading " + modId + " data..."), true);
        server.reloadResources(server.getPackRepository().getSelectedIds()).thenRun(() -> {
            server.execute(() -> {
                ReloadPipeline.reloadAll();
                ReloadGuardListener.reloadAndBroadcast(server);
                source.sendSuccess(() -> Component.literal(modId + " data reload complete."), true);
            });
        });
        return 1;
    }

    static int showDiagnostics(CommandContext<CommandSourceStack> ctx, String modId) {
        CommandSourceStack source = ctx.getSource();
        DatapackDiagnostics diagnostics = DatapackDiagnostics.getInstance();
        source.sendSuccess(() -> Component.literal("[" + modId + "] " + diagnostics.getSummary())
                .withStyle(ChatFormatting.GOLD), false);

        List<DatapackDiagnostic> allDiagnostics = diagnostics.getAll();
        int shown = 0;
        for (DatapackDiagnostic diagnostic : allDiagnostics) {
            if (shown >= MAX_DIAGNOSTICS_LINES) {
                break;
            }
            ChatFormatting color = diagnostic.severity() == DatapackDiagnostic.Severity.ERROR
                    ? ChatFormatting.RED
                    : ChatFormatting.YELLOW;
            source.sendSuccess(() -> Component.literal(diagnostic.toString()).withStyle(color), false);
            shown++;
        }

        int hidden = allDiagnostics.size() - shown;
        if (hidden > 0) {
            source.sendSuccess(
                    () -> Component.literal("... truncated " + hidden + " additional diagnostics")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    false);
        }
        return 1;
    }

    static int showSchemaTemplate(CommandContext<CommandSourceStack> ctx, String modId) {
        String type = StringArgumentType.getString(ctx, "type");
        SchemaDefinition schema = MarieCommandSupport.BUILTIN_SCHEMAS.stream()
                .filter(s -> s.getTypeName().equals(type))
                .findFirst()
                .orElse(null);
        if (schema == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown schema type: " + type));
            return 0;
        }

        String template = SchemaTemplateGenerator.generate(schema);
        ctx.getSource().sendSuccess(() -> Component.literal("Schema template for " + type + ":")
                .withStyle(ChatFormatting.GOLD), false);
        for (String line : template.split("\n")) {
            ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    static int repairGeneratedDatapack(CommandContext<CommandSourceStack> ctx, String modId) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT).normalize();
        Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(modId + "-generated").normalize();
        Path tagsDir = packRoot.resolve("data").resolve(modId).resolve("tags").resolve("item").resolve("values");

        try {
            MarieValidation.assertPathUnder(packRoot, worldRoot, "repairGeneratedDatapack");
            if (!Files.isDirectory(tagsDir)) {
                source.sendSuccess(() -> Component.literal(
                        "No " + modId + "-generated datapack value tags found."), false);
                return 1;
            }

            RepairSummary summary = repairGeneratedTagDirectory(tagsDir);
            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "Repaired %s-generated datapack: %d files scanned, %d entries made optional, %d retired files deleted. Run /reload before retesting tags.",
                    modId, summary.filesScanned(), summary.entriesConverted(), summary.filesDeleted()))
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (IOException | IllegalArgumentException | com.google.gson.JsonParseException ex) {
            MariesLib.LOGGER.error("[MarieLib] Failed to repair generated datapack", ex);
            source.sendFailure(Component.literal("Failed to repair " + modId + "-generated datapack: " + ex.getMessage()));
            return 0;
        }
    }

    private static RepairSummary repairGeneratedTagDirectory(Path tagsDir) throws IOException {
        Set<String> activeFiles = MarieCommandSupport.registeredValueKeys().stream()
                .map(key -> key + ".json")
                .collect(java.util.stream.Collectors.toSet());
        int filesScanned = 0;
        int entriesConverted = 0;
        int filesDeleted = 0;

        try (var stream = Files.list(tagsDir)) {
            for (Path tagFile : stream.toList()) {
                String fileName = tagFile.getFileName().toString();
                if (!Files.isRegularFile(tagFile) || !fileName.endsWith(".json")) {
                    continue;
                }
                if (!activeFiles.contains(fileName)) {
                    Files.delete(tagFile);
                    filesDeleted++;
                    continue;
                }

                filesScanned++;
                JsonObject root = JsonParser.parseString(Files.readString(tagFile, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray values = root.has("values") && root.get("values").isJsonArray()
                        ? root.getAsJsonArray("values")
                        : new JsonArray();
                JsonArray repairedValues = new JsonArray();
                boolean changed = false;

                for (JsonElement value : values) {
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        JsonObject optionalEntry = new JsonObject();
                        optionalEntry.addProperty("id", value.getAsString());
                        optionalEntry.addProperty("required", false);
                        repairedValues.add(optionalEntry);
                        entriesConverted++;
                        changed = true;
                    } else if (value.isJsonObject()) {
                        JsonObject entry = value.getAsJsonObject();
                        if (entry.has("id")) {
                            boolean needsRequired = !entry.has("required") || entry.get("required").getAsBoolean();
                            if (needsRequired) {
                                entry.addProperty("required", false);
                                entriesConverted++;
                                changed = true;
                            }
                        }
                        repairedValues.add(entry);
                    } else {
                        repairedValues.add(value);
                    }
                }

                if (!root.has("replace") || root.get("replace").getAsBoolean()) {
                    root.addProperty("replace", false);
                    changed = true;
                }
                if (changed) {
                    root.add("values", repairedValues);
                    Files.writeString(tagFile, GSON.toJson(root), StandardCharsets.UTF_8);
                }
            }
        }

        return new RepairSummary(filesScanned, entriesConverted, filesDeleted);
    }

    private record RepairSummary(int filesScanned, int entriesConverted, int filesDeleted) {}
}
