package dev.marie.MariesLib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPIState;
import dev.marie.MariesLib.api.MarieAPIVersion;
import dev.marie.MariesLib.config.ConfigValidatorRegistry;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MarieModRegistry;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.export.ExportResolverRegistry;
import dev.marie.MariesLib.export.ExportWriter;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import dev.marie.MariesLib.tagaudit.TagAuditReportWriter;
import dev.marie.MariesLib.tagaudit.TagScanner;
import dev.marie.MariesLib.tagaudit.model.TagAuditContext;
import dev.marie.MariesLib.tagaudit.model.TagReport;
import dev.marie.MariesLib.tagaudit.registry.TagAuditContextRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Library-only commands registered under /marieslib and /marie.
 * Most commands introspect the framework itself (status, mods, api, registries). {@code validate
 * <modid>} runs the generic config validation framework for one mod's registered validators,
 * the same way {@code dump <resolverId>} looks up one resolver's export — both require their
 * argument and do nothing when called bare.
 */
@ApiStatus.Internal
public final class MariesLibCommand {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        registerLibraryTree(dispatcher, "marie");
    }

    private void registerLibraryTree(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        dispatcher.register(
                Commands.literal(root)
                        .then(Commands.literal("status")
                                .executes(this::showStatus))
                        .then(Commands.literal("mods")
                                .executes(this::showMods))
                        .then(Commands.literal("api")
                                .executes(this::showApiPhase))
                        .then(Commands.literal("registries")
                                .executes(this::showRegistries))
                        .then(Commands.literal("dump")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("resolverId", StringArgumentType.string())
                                        .suggests((ctx, b) -> {
                                            ExportResolverRegistry.getAll().keySet().forEach(b::suggest);
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            String resolverId = StringArgumentType.getString(ctx, "resolverId");
                                            Supplier<Path> resolver = ExportResolverRegistry.get(resolverId);
                                            if (resolver == null) {
                                                source.sendFailure(Component.literal(
                                                        "[MariesLib] No exporter registered for: " + resolverId));
                                                return 0;
                                            }
                                            Path written = resolver.get();
                                            if (written == null) {
                                                source.sendFailure(Component.literal(
                                                        "[MariesLib] Export failed or produced no data for: " + resolverId));
                                                return 0;
                                            }
                                            source.sendSuccess(() -> Component.literal(
                                                    "[MariesLib] Wrote export to " + written), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("validate")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("modid", StringArgumentType.string())
                                        .suggests((ctx, b) -> {
                                            ConfigValidatorRegistry.getRegisteredModIds().forEach(b::suggest);
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            String modId = StringArgumentType.getString(ctx, "modid");
                                            Function<CommandContext<CommandSourceStack>, Integer> validator =
                                                    ConfigValidatorRegistry.get(modId);
                                            if (validator == null) {
                                                source.sendFailure(Component.literal(
                                                        "[MariesLib] No validator registered for modid: " + modId));
                                                return 0;
                                            }
                                            return validator.apply(ctx);
                                        })))
        );
    }

    private int showStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        String libVersion = ModList.get()
                .getModContainerById(MariesLib.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");

        String apiVersion = MarieAPIVersion.VERSION;
        int modCount = MarieModRegistry.getAll().size();
        String bootstrapMode = MarieLibContext.isRegistered() ? "consumer-driven" : "standalone";

        source.sendSuccess(() -> Component.literal("[MariesLib Status]").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Library version: " + libVersion).withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("API version: " + apiVersion).withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("Bootstrap mode: " + bootstrapMode).withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("Registered Marie mods: " + modCount).withStyle(ChatFormatting.WHITE), false);

        return 1;
    }

    private int showMods(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<MarieLibContext> mods = MarieModRegistry.getAll();

        if (mods.isEmpty()) {
            source.sendSuccess(() -> Component.literal("[MariesLib] No Marie mods registered.").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("[MariesLib Registered Mods]").withStyle(ChatFormatting.GOLD), false);

        MarieLibContext primary = MarieModRegistry.getPrimaryOrNull();
        for (MarieLibContext mod : mods) {
            String modId = mod.modId();
            boolean isPrimary = primary != null && modId.equals(primary.modId());
            String label = isPrimary ? modId + " (primary)" : modId;
            ChatFormatting color = isPrimary ? ChatFormatting.GREEN : ChatFormatting.WHITE;
            source.sendSuccess(() -> Component.literal("  - " + label).withStyle(color), false);
        }

        return 1;
    }

    private int showApiPhase(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MarieAPIState.Phase phase = MarieAPIState.getPhase();

        source.sendSuccess(() -> Component.literal("[MariesLib API]").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Registration phase: " + phase.name()).withStyle(ChatFormatting.WHITE), false);

        String hint;
        switch (phase) {
            case MOD_INIT -> hint = "Registrations allowed (mod initialization)";
            case DATAPACK_RELOAD -> hint = "Registrations allowed (datapack reload in progress)";
            case CLOSED -> hint = "Registrations closed (runtime)";
            default -> hint = "Unknown phase";
        }
        source.sendSuccess(() -> Component.literal("  " + hint).withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private int showRegistries(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<String> names = RegistryLifecycleManager.registeredNames();
        boolean reloading = RegistryLifecycleManager.isReloadInProgress();

        source.sendSuccess(() -> Component.literal("[MariesLib Registries]").withStyle(ChatFormatting.GOLD), false);

        if (reloading) {
            source.sendSuccess(() -> Component.literal("Reload in progress: YES").withStyle(ChatFormatting.YELLOW), false);
        } else {
            source.sendSuccess(() -> Component.literal("Reload in progress: no").withStyle(ChatFormatting.GREEN), false);
        }

        source.sendSuccess(() -> Component.literal("Registered lifecycle entries (" + names.size() + "):").withStyle(ChatFormatting.WHITE), false);
        for (String name : names) {
            source.sendSuccess(() -> Component.literal("  - " + name).withStyle(ChatFormatting.GRAY), false);
        }

        return 1;
    }

    private int runDump(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String resolverId = StringArgumentType.getString(ctx, "resolverId");

        Path written = ExportWriter.writeExport(resolverId);

        if (written == null) {
            source.sendFailure(Component.literal("[MariesLib] Export failed or produced no data for resolver: " + resolverId).withStyle(ChatFormatting.RED));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("[MariesLib] Wrote export to " + written).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private int runAuditTags(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String modId = StringArgumentType.getString(ctx, "modid");

        TagAuditContext context = TagAuditContextRegistry.get(modId);
        if (context == null) {
            source.sendFailure(Component.literal(
                    "[MariesLib] No TagAuditContext registered for modId: " + modId).withStyle(ChatFormatting.RED));
            return 0;
        }

        TagReport report = TagScanner.scan(context);
        Path reportPath = TagAuditReportWriter.write(modId, report);
        if (reportPath != null) {
            source.sendSuccess(() -> Component.literal(
                    "Full report written to " + reportPath.toAbsolutePath() + ".").withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        source.sendFailure(Component.literal("[MariesLib] Failed to write tag audit report.").withStyle(ChatFormatting.RED));
        return 0;
    }
}
