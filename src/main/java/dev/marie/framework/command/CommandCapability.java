package dev.marie.MariesLib.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.commands.CommandSourceStack;

@FunctionalInterface
@ApiStatus.Experimental
public interface CommandCapability {
    int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
}
