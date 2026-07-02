package dev.marie.framework.command;


import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.classification.ClassificationTrace;
import dev.marie.framework.classification.ClassificationTraceFormatter;
import dev.marie.framework.core.MariesLib;
import dev.marie.framework.runtime.RuntimeResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@ApiStatus.Internal
final class MarieConsumerCommandTree {

    private MarieConsumerCommandTree() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, String modId) {
        dispatcher.register(
                Commands.literal(modId)
                        .then(Commands.literal("report")
                                .executes(ctx -> MariePlayerCommands.reportSelf(ctx, modId))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(s -> s.hasPermission(2))
                                        .executes(ctx -> MariePlayerCommands.reportTarget(ctx, modId))))
                        .then(Commands.literal("value")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(MarieCommandSupport.VALUE_SUGGESTIONS)
                                        .executes(ctx -> MariePlayerCommands.valueSelf(ctx, modId))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(s -> s.hasPermission(2))
                                                .executes(ctx -> MariePlayerCommands.valueTarget(ctx, modId)))))
                        .then(Commands.literal("set")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(MarieCommandSupport.VALUE_SUGGESTIONS)
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 1f))
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(ctx -> MariePlayerCommands.setValue(ctx, modId))))))
                        .then(Commands.literal("set_all")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 1f))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> MariePlayerCommands.setAllValues(ctx, modId)))))
                        .then(Commands.literal("reset")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> MariePlayerCommands.resetPlayer(ctx, modId))))
                        .then(Commands.literal("profile")
                                .then(Commands.literal("list").executes(ctx -> MariePlayerCommands.profileList(ctx, modId)))
                                .then(Commands.literal("set")
                                        .requires(s -> s.hasPermission(2))
                                        .then(Commands.argument("profile", StringArgumentType.word())
                                                .suggests(MarieCommandSupport.PROFILE_SUGGESTIONS)
                                                .executes(ctx -> MariePlayerCommands.profileSetSelf(ctx, modId))
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .requires(s -> s.hasPermission(2))
                                                        .executes(ctx -> MariePlayerCommands.profileSetTarget(ctx, modId)))))
                                .then(Commands.literal("get")
                                        .executes(ctx -> MariePlayerCommands.profileGetSelf(ctx, modId))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(s -> s.hasPermission(2))
                                                .executes(ctx -> MariePlayerCommands.profileGetTarget(ctx, modId)))))
                        .then(Commands.literal("nbt")
                                .executes(ctx -> MarieScannerCommands.showNbtPaths(ctx, modId)))
                        .then(Commands.literal("get_unassigned")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> MarieScannerCommands.showUnassignedSources(ctx, modId)))
                        .then(Commands.literal("reload")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> MarieDatapackCommands.reloadAll(ctx, modId)))
                        .then(Commands.literal("invalidatecache")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> MarieScannerCommands.invalidateCache(ctx, modId)))
                        .then(Commands.literal("repair_generated_datapack")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> MarieDatapackCommands.repairGeneratedDatapack(ctx, modId)))
                        .then(Commands.literal("generate_milestone_template")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> MarieMilestoneTemplateCommand.generate(ctx, modId)))
                        .then(Commands.literal("diagnostics")
                                .executes(ctx -> MarieDatapackCommands.showDiagnostics(ctx, modId)))
                        .then(Commands.literal("validate")
                                .executes(ctx -> MarieValidationCommands.runForModId(ctx, modId)))
                        .then(Commands.literal("scan_analysis")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> MarieScannerCommands.runScanAnalysis(ctx, modId)))
                        .then(Commands.literal("scan")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> MarieScannerCommands.runScanAnalysis(ctx, modId)))
                        .then(Commands.literal("schema")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(MarieCommandSupport.SCHEMA_TYPE_SUGGESTIONS)
                                        .executes(ctx -> MarieDatapackCommands.showSchemaTemplate(ctx, modId))))
                        .then(Commands.literal("debug")
                                .requires(s -> s.hasPermission(2))
                                .then(MarieDebugCommand.registerCache(modId))
                                .then(MarieDebugCommand.registerHeld(modId))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> MariePlayerCommands.debugTarget(ctx, modId))))
                        .then(Commands.literal("analyze")
                                .requires(s -> s.hasPermission(0))
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> {
                                            BuiltInRegistries.ITEM.keySet().stream()
                                                    .filter(rl -> {
                                                        Item it = BuiltInRegistries.ITEM.get(rl);
                                                        return it != null && it != Items.AIR
                                                                && it.components().has(DataComponents.FOOD);
                                                    })
                                                    .forEach(rl -> builder.suggest(rl.toString()));
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            ResourceLocation itemId = ResourceLocationArgument.getId(ctx, "item");
                                            Item item = BuiltInRegistries.ITEM.get(itemId);

                                            if (item == null || item == Items.AIR) {
                                                source.sendFailure(Component.literal("Unknown item: " + itemId));
                                                return 0;
                                            }

                                            ItemStack stack = new ItemStack(item);
                                            RecipeManager recipeManager = source.getServer().getRecipeManager();
                                            ClassificationTrace trace = RuntimeResolver.getInstance()
                                                    .resolveWithTrace(stack, recipeManager);

                                            if (trace == null) {
                                                source.sendFailure(Component.literal(
                                                        itemId + " could not be analyzed — not a food or not resolvable."));
                                                return 0;
                                            }

                                            String inspectorOutput = ClassificationTraceFormatter.format(trace, stack);
                                            Instant dumpedAt = Instant.now();
                                            String fullOutput = "Item ID: " + itemId.toString()
                                                    + "\nDisplay Name: " + stack.getHoverName().getString()
                                                            .replace('\r', ' ').replace('\n', ' ')
                                                    + "\nTimestamp: " + DateTimeFormatter.ISO_INSTANT.format(dumpedAt)
                                                    + "\n\n" + inspectorOutput;

                                            try {
                                                Path dir = FMLPaths.CONFIGDIR.get().resolve(modId).resolve("debug");
                                                Files.createDirectories(dir);
                                                String safeItemName = itemId.toString().replace(':', '_');
                                                String timestamp = java.time.format.DateTimeFormatter
                                                        .ofPattern("yyyyMMdd'T'HHmmss")
                                                        .withZone(java.time.ZoneOffset.UTC)
                                                        .format(dumpedAt);
                                                Path file = dir.resolve("analyze_" + safeItemName + "_" + timestamp + ".txt");
                                                Files.writeString(file, fullOutput);
                                                source.sendSuccess(() -> Component.literal(
                                                        "Analysis written to: " + file.toString()), false);
                                            } catch (IOException e) {
                                                MariesLib.LOGGER.warn("[MarieConsumerCommandTree] Failed to write analyze dump: {}",
                                                        e.getMessage());
                                                source.sendFailure(Component.literal("Analyze write failed: " + e.getMessage()));
                                                return 0;
                                            }
                                            return 1;
                                        })))
        );
    }
}
