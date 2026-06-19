package dev.marie.MariesLib.command;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ReportProvider;
import dev.marie.MariesLib.api.registry.ReportProviderRegistry;
import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.marie.MariesLib.tracking.TrackingData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared formatting and permission utilities for MarieLib commands.
 */
@ApiStatus.Internal
public final class MarieCommandSource {

    private static final int BAR_WIDTH = 8;

    private MarieCommandSource() {}

    public enum ValueStatus {
        CRITICAL,
        LOW,
        OK,
        EXCESS
    }

    public static boolean canTargetOtherPlayer(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer self = source.getPlayer();
        if (self == null) {
            return true;
        }
        return self.getUUID().equals(target.getUUID()) || source.hasPermission(2);
    }

    public static ValueStatus statusFor(float value, String key) {
        float critical = IMarieLibConfig.get().criticalThresholdFor(key);
        float low = IMarieLibConfig.get().lowThreshold();
        float excess = IMarieLibConfig.get().excessThreshold();
        if (value < critical) return ValueStatus.CRITICAL;
        if (value < low) return ValueStatus.LOW;
        if (value > excess) return ValueStatus.EXCESS;
        return ValueStatus.OK;
    }

    public static Component statusChip(ValueStatus status) {
        return switch (status) {
            case CRITICAL -> chip("CRITICAL", ChatFormatting.RED);
            case LOW -> chip("LOW", ChatFormatting.YELLOW);
            case OK -> chip("OK", ChatFormatting.GREEN);
            case EXCESS -> chip("EXCESS", ChatFormatting.AQUA);
        };
    }

    public static List<Component> buildReportLines(
            ServerPlayer target, TrackingData data, String activeProfile) {
        return buildReportLines(target, data, activeProfile,
                MarieCommandSource::decayRateFor,
                (value, key) -> statusFor(value, key));
    }

    private static List<Component> buildReportLines(
            ServerPlayer target,
            TrackingData data,
            String activeProfile,
            java.util.function.Function<String, Float> decayRateFor,
            java.util.function.BiFunction<Float, String, ValueStatus> statusFor) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("=== " + IMarieLibConfig.get().modId() + " Report ===").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("Player: ").withStyle(ChatFormatting.GRAY)
                .append(target.getName().copy().withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal("Active Profile: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(activeProfile).withStyle(ChatFormatting.LIGHT_PURPLE)));
        lines.add(Component.literal("Total: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(java.util.Locale.ROOT, "%.0f", data.total)).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal(" "));

        for (String key : data.values.keySet()) {
            float value = data.values.getOrDefault(key, 0f);
            float decayRate = decayRateFor.apply(key);
            ValueStatus status = statusFor.apply(value, key);

            MutableComponent line = Component.literal(key + ": ").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(bar(value) + " "))
                    .append(Component.literal(String.format(java.util.Locale.ROOT, "%3d%% ", Math.round(value * 100f))).withStyle(ChatFormatting.GRAY))
                    .append(statusChip(status))
                    .append(Component.literal("  decay=" + String.format(java.util.Locale.ROOT, "%.4f", decayRate)).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(line);
        }

        for (ReportProvider provider : ReportProviderRegistry.getAll()) {
            lines.add(Component.literal(" "));
            lines.add(provider.getSectionTitle().copy().withStyle(ChatFormatting.GOLD));
            List<Component> sectionLines = provider.generateReport(target);
            for (Component sectionLine : sectionLines) {
                lines.add(Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY).append(sectionLine.copy()));
            }
        }

        return lines;
    }

    private static float decayRateFor(String key) {
        return IMarieLibConfig.get().decayRateFor(key);
    }

    private static Component chip(String text, ChatFormatting color) {
        return Component.literal("[" + text + "]").withStyle(color);
    }

    private static String bar(float value) {
        int filled = Math.round(Math.max(0f, Math.min(1f, value)) * BAR_WIDTH);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < BAR_WIDTH; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        sb.append("]");
        return sb.toString();
    }
}
