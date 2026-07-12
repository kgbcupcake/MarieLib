package dev.marie.framework.network;

import dev.marie.framework.api.ApiStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Common/server-side registration for the block-hover request/response channel.
 * Skips entirely on the client dist — {@link BlockHoverClientNetworking} owns that side.
 */
@ApiStatus.Internal
public final class BlockHoverServerNetworking {

    private BlockHoverServerNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return;
        }

        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                BlockHoverRequestPayload.TYPE,
                BlockHoverRequestPayload.STREAM_CODEC,
                BlockHoverRequestHandler::handle
        );

        registrar.playToClient(
                BlockHoverResponsePayload.TYPE,
                BlockHoverResponsePayload.STREAM_CODEC,
                (payload, context) -> {}
        );
    }
}
