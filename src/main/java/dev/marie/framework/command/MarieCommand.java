package dev.marie.framework.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieLibContext;
import dev.marie.framework.core.MarieModRegistry;
import dev.marie.framework.core.MariesLib;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@ApiStatus.Internal
public class MarieCommand {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        for (MarieLibContext ctx : MarieModRegistry.getAll()) {
            String modId = ctx.modId();
            if (!modId.equals(MariesLib.MOD_ID)) {
                MarieConsumerCommandTree.register(dispatcher, modId);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MarieCommandSupport.onPlayerLoggedOut(player);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        MarieCommandSupport.onServerStopped();
    }

}
