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
 *
 * <p><b>Warning:</b> {@code data} is fully client-controlled, untrusted input. MarieLib validates
 * the position (loaded chunk, in reach) and applies a size/rate ceiling before dispatching to
 * handlers, but it does not and cannot validate the tag's contents or schema. Handler authors
 * must independently verify ownership/permission and the tag's expected shape before acting on
 * it — e.g. re-checking that the block at {@code pos} is still the expected type before trusting
 * it, the way Thermal Systems' {@code EnderIOIntegration} re-validates block type before trusting
 * a synced position.</p>
 *
 * <p>{@code oversized} is a decode-time-only signal, never written to the wire: the decoder sets
 * it to {@code true} (pairing it with an empty placeholder tag) when the raw NBT byte count on
 * the wire exceeds {@link MarieNetworking}'s size ceiling, so the tag is never actually decoded.
 * Callers must never construct an instance with {@code oversized = true} themselves.</p>
 */
@ApiStatus.Experimental
public record GenericStateSyncPayload(BlockPos pos, CompoundTag data, boolean oversized)
        implements CustomPacketPayload {

    public static final Type<GenericStateSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MarieCore.MOD_ID, "generic_state_sync"));

    public static final StreamCodec<ByteBuf, GenericStateSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                ByteBufCodecs.COMPOUND_TAG.encode(buf, payload.data());
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                if (buf.readableBytes() > MarieNetworking.maxPayloadBytes()) {
                    buf.skipBytes(buf.readableBytes());
                    return new GenericStateSyncPayload(pos, new CompoundTag(), true);
                }
                CompoundTag data = ByteBufCodecs.COMPOUND_TAG.decode(buf);
                return new GenericStateSyncPayload(pos, data, false);
            });

    public GenericStateSyncPayload {
        assert oversized == (data instanceof CompoundTag t && t.isEmpty()) || !oversized
                : "oversized=true must pair with an empty placeholder tag";
    }

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
        PacketDistributor.sendToServer(new GenericStateSyncPayload(pos, data, false));
    }
}
