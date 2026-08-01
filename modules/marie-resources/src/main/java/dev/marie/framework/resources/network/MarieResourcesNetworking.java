package dev.marie.framework.resources.network;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.network.SyncState;
import dev.marie.framework.resources.registry.ClientConfigSyncStateRegistry;
import dev.marie.framework.resources.registry.ConfigSyncClientHandlerRegistry;
import dev.marie.framework.resources.registry.ConfigSyncSupplierRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registers marie-resources's own generic config-sync packet channel, sends every registered
 * registry's snapshot to players on login, and dispatches inbound
 * {@link GenericConfigSyncPayload} instances on the client to whatever handler the consuming mod
 * registered via {@link dev.marie.framework.resources.api.MarieResourcesAPI}.
 *
 * <p>Self-registers against marie-core's mod bus and game bus via {@link EventBusSubscriber} —
 * core has no compile-time dependency on marie-resources, per the locked one-directional module
 * graph.</p>
 */
@ApiStatus.Internal
@EventBusSubscriber(modid = MarieCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class MarieResourcesNetworking {

    private MarieResourcesNetworking() {}

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MarieCore.MOD_ID).versioned("1");
        registrar.playToClient(
                GenericConfigSyncPayload.TYPE,
                GenericConfigSyncPayload.STREAM_CODEC,
                MarieResourcesNetworking::handleClient);
    }

    private static void handleClient(GenericConfigSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            String registryId = payload.registryId();
            Consumer<CompoundTag> handler = ConfigSyncClientHandlerRegistry.instance().get(registryId);
            if (handler == null) {
                MarieCore.LOGGER.warn(
                        "[MarieResources] Ignoring config sync for unregistered registryId: {}", registryId);
                return;
            }
            ClientConfigSyncStateRegistry.set(registryId, SyncState.PENDING);
            handler.accept(payload.data());
            ClientConfigSyncStateRegistry.set(registryId, SyncState.ACTIVE);
        });
    }

    static void sendAllTo(ServerPlayer player) {
        for (var entry : ConfigSyncSupplierRegistry.instance().entries().entrySet()) {
            sendSnapshotTo(player, entry.getKey(), entry.getValue());
        }
    }

    private static void sendSnapshotTo(ServerPlayer player, String registryId, Supplier<CompoundTag> supplier) {
        CompoundTag snapshot = supplier.get();
        PacketDistributor.sendToPlayer(player, new GenericConfigSyncPayload(registryId, snapshot));
    }

    /**
     * Rebuilds one registry's snapshot and resends it to every connected player.
     */
    public static void broadcastReload(MinecraftServer server, String registryId) {
        Supplier<CompoundTag> supplier = ConfigSyncSupplierRegistry.instance().get(registryId);
        if (supplier == null) {
            MarieCore.LOGGER.warn(
                    "[MarieResources] No config sync supplier registered for registryId: {}", registryId);
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSnapshotTo(player, registryId, supplier);
        }
    }
}
