package dev.marie.framework.network;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Generic client-to-server sync packet: a {@link BlockPos} plus an opaque {@link CompoundTag}.
 *
 * <p>MarieLib does not interpret the tag's contents — a consuming mod uses this to sync an
 * arbitrary small piece of block-associated state to the server without defining its own
 * {@link CustomPacketPayload}, registering its own channel, or hooking
 * {@code RegisterPayloadHandlersEvent} itself. Register a server-side handler via
 * {@link dev.marie.framework.api.marieapi.MarieAPI#registerGenericStateSyncHandler}, then call
 * {@link #sendToServer} from client code.</p>
 *
 * <p>For state that doesn't fit "a tag keyed by a block position" (e.g. entity-scoped state,
 * larger payloads), a consuming mod should still define its own {@link CustomPacketPayload} —
 * this type is intentionally narrow, not a universal envelope.</p>
 */
@ApiStatus.Experimental
public record GenericStateSyncPayload(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

    public static final Type<GenericStateSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MarieCore.MOD_ID, "generic_state_sync"));

    public static final StreamCodec<ByteBuf, GenericStateSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GenericStateSyncPayload::pos,
            ByteBufCodecs.COMPOUND_TAG, GenericStateSyncPayload::data,
            GenericStateSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Sends this payload to the server. Must be called client-side.
     *
     * @param pos  the block position this state is associated with
     * @param data the opaque state to sync — the receiving handler defines its meaning
     */
    public static void sendToServer(BlockPos pos, CompoundTag data) {
        PacketDistributor.sendToServer(new GenericStateSyncPayload(pos, data));
    }
}
