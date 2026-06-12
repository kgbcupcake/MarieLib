package dev.marie.MariesLib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.marie.MariesLib.api.MarieAPIState;
import dev.marie.MariesLib.api.MarieAPIVersion;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MarieModRegistry;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/**
 * Library-only commands registered under /marieslib and /marie.
 * These introspect the framework itself, not consumer mod data.
 */
public final class MariesLibCommand {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        registerLibraryTree(dispatcher, MariesLib.MOD_ID);
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
}
