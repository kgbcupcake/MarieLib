package dev.marie.framework.network;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieBootstrap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Client-dist registration for the block-hover request/response channel. Client is the
 * sender of the request and receiver of the response; the response handler delegates to
 * whatever marie-ui installed via {@link MarieBootstrap#setBlockHoverResponseListener}.
 */
@ApiStatus.Internal
public final class BlockHoverClientNetworking {

    private BlockHoverClientNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                BlockHoverRequestPayload.TYPE,
                BlockHoverRequestPayload.STREAM_CODEC,
                (payload, context) -> {}
        );

        registrar.playToClient(
                BlockHoverResponsePayload.TYPE,
                BlockHoverResponsePayload.STREAM_CODEC,
                (payload, context) -> MarieBootstrap.getBlockHoverResponseListener().accept(payload)
        );
    }
}
