package dev.marie.framework.network;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.registry.GenericStateSyncHandlerRegistry;
import dev.marie.framework.core.MarieCore;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.BiConsumer;

/**
 * Registers MarieLib's own generic sync packet channel and dispatches inbound
 * {@link GenericStateSyncPayload} instances to whatever handlers consuming mods registered via
 * {@link dev.marie.framework.api.marieapi.MarieAPI#registerGenericStateSyncHandler}.
 *
 * <p>Internal — consuming mods never call this directly.</p>
 */
@ApiStatus.Internal
public final class MarieNetworking {

    private MarieNetworking() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MarieNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MarieCore.MOD_ID).versioned("1");
        registrar.playToServer(
                GenericStateSyncPayload.TYPE,
                GenericStateSyncPayload.STREAM_CODEC,
                MarieNetworking::handleServer);
    }

    private static void handleServer(GenericStateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                if (!serverPlayer.level().isLoaded(payload.pos())) {
                    MarieCore.LOGGER.debug(
                            "[MarieLib] Ignoring GenericStateSyncPayload from {} for unloaded chunk at {}",
                            serverPlayer.getGameProfile().getName(), payload.pos());
                    return;
                }
                if (!serverPlayer.canInteractWithBlock(payload.pos(), 0.0)) {
                    MarieCore.LOGGER.debug(
                            "[MarieLib] Ignoring GenericStateSyncPayload from {} for out-of-reach position {}",
                            serverPlayer.getGameProfile().getName(), payload.pos());
                    return;
                }
                for (BiConsumer<ServerPlayer, GenericStateSyncPayload> handler
                        : GenericStateSyncHandlerRegistry.getAll()) {
                    handler.accept(serverPlayer, payload);
                }
            }
        });
    }
}
