package dev.marie.MariesLib.command;

import dev.marie.MariesLib.api.ApiStatus;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

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
                        .then(Commands.literal("diagnostics")
                                .executes(ctx -> MarieDatapackCommands.showDiagnostics(ctx, modId)))
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
        );
    }
}
