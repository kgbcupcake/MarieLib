package dev.marie.framework.resources.network;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Generic server-to-client sync packet: a registry id plus an opaque {@link CompoundTag} snapshot.
 *
 * <p>MarieLib does not interpret the tag's contents — a consuming mod uses this to sync the full
 * state of one server-loaded JSON registry to clients on login or reload, without defining its own
 * {@link CustomPacketPayload}, registering its own channel, or hooking
 * {@code RegisterPayloadHandlersEvent} itself. Register a supplier and client handler via
 * {@link dev.marie.framework.resources.api.MarieResourcesAPI}.</p>
 */
@ApiStatus.Experimental
public record GenericConfigSyncPayload(String registryId, CompoundTag data) implements CustomPacketPayload {

    public static final Type<GenericConfigSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MarieCore.MOD_ID, "generic_config_sync"));

    public static final StreamCodec<ByteBuf, GenericConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32767), GenericConfigSyncPayload::registryId,
            ByteBufCodecs.COMPOUND_TAG, GenericConfigSyncPayload::data,
            GenericConfigSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
