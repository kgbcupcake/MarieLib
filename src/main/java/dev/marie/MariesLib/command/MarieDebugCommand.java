package dev.marie.MariesLib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marie.MariesLib.classification.ClassificationTrace;
import dev.marie.MariesLib.classification.ClassificationTraceFormatter;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.runtime.RuntimeResolver;
import dev.marie.MariesLib.scan.CacheStats;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MarieDebugCommand {

    private static final int CACHE_MAX = 2048;
    private static final String DEBUG_SUBDIR = "debug";

    private MarieDebugCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> registerHeld() {
        return Commands.literal("held")
                .requires(s -> s.hasPermission(2))
                .executes(MarieDebugCommand::debugHeld);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerCache() {
        return Commands.literal("cache")
                .requires(s -> s.hasPermission(2))
                .executes(MarieDebugCommand::executeDebugCache);
    }

    private static int executeDebugCache(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        CacheStats stats = RuntimeResolver.getInstance().getCacheStats();
        sendCacheStatsFeedback(source, stats);
        return 1;
    }

    private static void sendCacheStatsFeedback(CommandSourceStack source, CacheStats stats) {
        source.sendSuccess(() -> Component.literal("[MarieLib Cache Stats]").withStyle(ChatFormatting.GOLD), false);
        sendCacheKeyValue(source, "Hits        ", String.valueOf(stats.hits()), ChatFormatting.WHITE);
        sendCacheKeyValue(source, "Misses      ", String.valueOf(stats.misses()), ChatFormatting.WHITE);
        sendHitRatioLine(source, stats);
        sendCacheKeyValue(source, "Cache Size  ", stats.size() + " / " + CACHE_MAX, ChatFormatting.WHITE);
        sendAvgResolveLine(source, stats);
        sendSlowestLine(source, stats);
        sendTimeoutsLine(source, stats);
    }

    private static void sendAvgResolveLine(CommandSourceStack source, CacheStats stats) {
        float ms = stats.avgResolveNanos() / 1_000_000f;
        String msText = String.format(Locale.ROOT, "%.2fms", ms);
        ChatFormatting color;
        if (ms > 5f) {
            color = ChatFormatting.RED;
        } else if (ms > 2f) {
            color = ChatFormatting.YELLOW;
        } else {
            color = ChatFormatting.GREEN;
        }
        MutableComponent line = Component.literal("Avg Resolve : ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(msText).withStyle(color));
        source.sendSuccess(() -> line, false);
    }

    private static void sendSlowestLine(CommandSourceStack source, CacheStats stats) {
        float ms = stats.slowestResolveNanos() / 1_000_000f;
        String msText = String.format(Locale.ROOT, "%.2fms", ms);
        ResourceLocation item = stats.slowestItem();
        MutableComponent line = Component.literal("Slowest     : ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(msText).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  (").withStyle(ChatFormatting.GRAY));
        if (item == null) {
            line.append(Component.literal("N/A").withStyle(ChatFormatting.GRAY));
        } else {
            line.append(Component.literal(item.toString()).withStyle(ChatFormatting.WHITE));
        }
        line.append(Component.literal(")").withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> line, false);
    }

    private static void sendTimeoutsLine(CommandSourceStack source, CacheStats stats) {
        int timeouts = stats.recipeTimeouts();
        ChatFormatting color = timeouts >= 1 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        sendCacheKeyValue(source, "Timeouts    ", String.valueOf(timeouts), color);
    }

    private static void sendCacheKeyValue(CommandSourceStack source, String label, String value, ChatFormatting valueColor) {
        MutableComponent line = Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(valueColor));
        source.sendSuccess(() -> line, false);
    }

    private static void sendHitRatioLine(CommandSourceStack source, CacheStats stats) {
        int total = stats.hits() + stats.misses();
        MutableComponent line = Component.literal("Hit Ratio   : ").withStyle(ChatFormatting.GRAY);
        if (total == 0) {
            line.append(Component.literal("N/A").withStyle(ChatFormatting.GRAY));
        } else {
            float hitRatio = (float) stats.hits() / total * 100f;
            String ratioText = String.format(Locale.ROOT, "%.2f%%", hitRatio);
            ChatFormatting ratioColor;
            if (hitRatio >= 80f) {
                ratioColor = ChatFormatting.GREEN;
            } else if (hitRatio >= 50f) {
                ratioColor = ChatFormatting.YELLOW;
            } else {
                ratioColor = ChatFormatting.RED;
            }
            line.append(Component.literal(ratioText).withStyle(ratioColor));
        }
        source.sendSuccess(() -> line, false);
    }

    private static int debugHeld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No item in main hand."), false);
            return 0;
        }

        RecipeManager recipeManager = source.getServer().getRecipeManager();
        String traceOutput = MarieLibContext.get().heldItemTraceProvider().apply(stack, recipeManager);
        ClassificationTrace classTrace =
                MarieLibContext.get().heldItemClassificationTraceProvider().apply(stack, recipeManager);
        String inspectorOutput = classTrace != null
                ? ClassificationTraceFormatter.format(classTrace, stack)
                : "";

        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack.getItem());
        String itemName = itemId != null ? itemId.toString() : "unknown";

        Instant dumpedAt = Instant.now();
        String fullTrace = "Item ID: " + itemName
                + "\nDisplay Name: " + singleLineForDump(stack.getHoverName().getString())
                + "\nTimestamp: " + DateTimeFormatter.ISO_INSTANT.format(dumpedAt)
                + "\n\n" + traceOutput
                + "\n\n---\n\n" + inspectorOutput;

        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve(MarieLibContext.get().modId()).resolve(DEBUG_SUBDIR);
            Files.createDirectories(dir);
            Path file = dir.resolve("trace_dump.txt");
            Files.writeString(file, fullTrace);
            source.sendSuccess(() -> Component.literal("Trace written to: " + file.toString()), false);
        } catch (IOException e) {
            MariesLib.LOGGER.warn("[MarieDebugCommand] Failed to write trace dump: {}", e.getMessage());
            source.sendSuccess(() -> Component.literal("Trace write failed: " + e.getMessage()), false);
        }

        return 1;
    }

    /** Collapses line breaks so trace dump header stays single-line per field. */
    private static String singleLineForDump(String s) {
        return s == null ? "" : s.replace('\r', ' ').replace('\n', ' ');
    }

    private static String sanitizeForFilename(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static void sendSection(CommandSourceStack source, String title) {
        source.sendSuccess(() -> Component.literal(title).withStyle(ChatFormatting.YELLOW), false);
    }

    private static void sendKeyValue(CommandSourceStack source, String label, String value) {
        MutableComponent line = Component.literal(label).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
        source.sendSuccess(() -> line, false);
    }

    private static void sendDarkGray(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.DARK_GRAY), false);
    }

    private static void sendBlank(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(" "), false);
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
