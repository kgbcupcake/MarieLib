package dev.marie.framework.command;

// TODO(marie-core migration): depends on dev.marie.framework.{api} (not yet migrated to marie-core; module will not compile until that lands)

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marie.framework.api.ApiStatus;
import net.minecraft.commands.CommandSourceStack;

@FunctionalInterface
@ApiStatus.Experimental
public interface CommandCapability {
    int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
}
