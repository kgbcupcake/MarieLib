package dev.marie.framework.command;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.value.ValueDefinition;
import dev.marie.framework.api.registry.ValueRegistry;
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

/**
 * Writes a starter milestone + advancement datapack template to
 * {@code <world>/datapacks/<modid>-milestone-template/}, so datapack authors
 * have a working example to copy and edit.
 */
@ApiStatus.Internal
final class MarieMilestoneTemplateCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PLACEHOLDER_VALUE_KEY = "your_value_key_here";

    private MarieMilestoneTemplateCommand() {}

    static int generate(CommandContext<CommandSourceStack> ctx, String modId) {
        CommandSourceStack source = ctx.getSource();
        if (!MarieCommandSupport.ensureConsumerRegistered(source)) {
            return 0;
        }

        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT).normalize();
        Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR)
                .resolve(modId + "-milestone-template").normalize();

        try {
            MarieValidation.assertPathUnder(packRoot, worldRoot, "generateMilestoneTemplate");

            String exampleValueKey = firstRegisteredValueKey();

            Path milestonesDir = packRoot.resolve("data").resolve(modId).resolve(modId).resolve("milestones");
            Path advancementsDir = packRoot.resolve("data").resolve(modId).resolve("advancement").resolve("milestones");
            Files.createDirectories(milestonesDir);
            Files.createDirectories(advancementsDir);

            writePackMeta(packRoot.resolve("pack.mcmeta"), modId);
            writeMilestoneJson(milestonesDir.resolve("example.json"), modId, exampleValueKey);
            writeAdvancementRoot(advancementsDir.resolve("root.json"));
            writeAdvancementExample(advancementsDir.resolve("example.json"), modId);

            source.sendSuccess(() -> Component.literal(
                            "Generated milestone template at " + packRoot.toAbsolutePath()
                                    + " (value_key: " + exampleValueKey + "). Run /reload to load it.")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (IOException | IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Failed to generate milestone template: " + ex.getMessage()));
            return 0;
        }
    }

    private static String firstRegisteredValueKey() {
        List<String> keys = ValueRegistry.getAll().stream().map(ValueDefinition::getId).toList();
        return keys.isEmpty() ? PLACEHOLDER_VALUE_KEY : keys.get(0);
    }

    private static void writePackMeta(Path path, String modId) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 48);
        pack.addProperty("description", modId + " milestone template (example only)");
        root.add("pack", pack);
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static void writeMilestoneJson(Path path, String modId, String valueKey) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_comment_value_key", "one of the registered value keys");
        root.addProperty("value_key", valueKey);
        root.addProperty("_comment_cumulative_goal",
                "cumulative sum of raw consumed amount units for this value over time "
                        + "(same units as source_classifications' amount field, scaled by the value's amountScale) "
                        + "— not a 0.0-1.0 normalized level like thresholds");
        root.addProperty("cumulative_goal", 5.0f);
        root.addProperty("_comment_reward_effect_id",
                "any vanilla or modded status effect id, e.g. minecraft:regeneration");
        root.addProperty("reward_effect_id", "minecraft:regeneration");
        root.addProperty("_comment_amplifier",
                "0-indexed effect level — 0 = level I, 1 = level II, 2 = level III, etc.");
        root.addProperty("amplifier", 0);
        root.addProperty("_comment_reward_duration", "ticks — 20 ticks = 1 second");
        root.addProperty("reward_duration", 200);
        root.addProperty("_comment_advancement_id",
                "resource location of the vanilla advancement to grant when this milestone triggers, "
                        + "e.g. nourished:milestones/example");
        root.addProperty("advancement_id", modId + ":milestones/example");
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static void writeAdvancementRoot(Path path) throws IOException {
        String json = """
                {
                  "criteria": {
                    "impossible": { "trigger": "minecraft:impossible" }
                  },
                  "display": {
                    "icon": { "id": "minecraft:apple" },
                    "title": { "text": "Milestones" },
                    "description": { "text": "Milestone template root" },
                    "frame": "task",
                    "show_toast": false,
                    "announce_to_chat": false,
                    "hidden": true
                  }
                }
                """;
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private static void writeAdvancementExample(Path path, String modId) throws IOException {
        String json = """
                {
                  "parent": "%s:milestones/root",
                  "criteria": {
                    "impossible": { "trigger": "minecraft:impossible" }
                  },
                  "display": {
                    "icon": { "id": "minecraft:apple" },
                    "title": { "text": "Example Milestone" },
                    "description": { "text": "Replace with your own milestone" },
                    "frame": "goal",
                    "show_toast": true,
                    "announce_to_chat": true
                  }
                }
                """.formatted(modId);
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }
}
