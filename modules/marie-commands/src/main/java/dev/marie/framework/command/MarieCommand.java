package dev.marie.framework.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.core.MarieModRegistry;
import dev.marie.framework.core.MarieCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Auto-subscribed to the game event bus via {@link EventBusSubscriber} so that
 * marie-core never has to name this class directly — core has no compile-time
 * dependency on marie-commands, per the locked one-directional module graph.
 */
@ApiStatus.Internal
@EventBusSubscriber(modid = MarieCore.MOD_ID)
public final class MarieCommand {

    private MarieCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        for (MarieContext ctx : MarieModRegistry.getAll()) {
            String modId = ctx.modId();
            if (!modId.equals(MarieCore.MOD_ID)) {
                MarieConsumerCommandTree.register(dispatcher, modId);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MarieCommandSupport.onPlayerLoggedOut(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MarieCommandSupport.onServerStopped();
    }

}
