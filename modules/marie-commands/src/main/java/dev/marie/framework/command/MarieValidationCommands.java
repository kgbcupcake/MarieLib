package dev.marie.framework.command;

// TODO(marie-core migration): depends on dev.marie.framework.{api, config} (not yet migrated to marie-core; module will not compile until that lands)

import com.mojang.brigadier.context.CommandContext;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.config.validation.Finding;
import dev.marie.framework.config.validation.ValidationResult;
import dev.marie.framework.config.validation.ValidationRunner;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;

@ApiStatus.Internal
final class MarieValidationCommands {

    private MarieValidationCommands() {}

    static int runForModId(CommandContext<CommandSourceStack> ctx, String modId) {
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal("[" + modId + "] Config validation")
                .withStyle(ChatFormatting.GOLD), false);

        printResults(source, ValidationRunner.runForMod(modId));
        return 1;
    }

    private static void printResults(CommandSourceStack source, List<ValidationResult> results) {
        if (results.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No config validators registered.")
                    .withStyle(ChatFormatting.GRAY), false);
            return;
        }

        for (ValidationResult result : results) {
            ChatFormatting statusColor = switch (result.status()) {
                case PASS -> ChatFormatting.GREEN;
                case WARN -> ChatFormatting.YELLOW;
                case FAIL -> ChatFormatting.RED;
            };
            int findingCount = result.findings().size();
            source.sendSuccess(() -> Component.literal("[" + result.validatorId() + "] "
                            + result.status().name() + " (" + findingCount + " findings)")
                    .withStyle(statusColor), false);

            if (result.status() != ValidationResult.Status.PASS) {
                for (Finding finding : result.findings()) {
                    String detail = finding.key() != null
                            ? finding.file() + ": " + finding.key() + " — " + finding.message()
                            : finding.file() + " — " + finding.message();
                    source.sendSuccess(() -> Component.literal("  " + detail).withStyle(ChatFormatting.GRAY), false);
                }
            }
        }
    }
}
