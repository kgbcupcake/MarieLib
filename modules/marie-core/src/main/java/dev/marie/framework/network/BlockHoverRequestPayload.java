package dev.marie.framework.network;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import dev.marie.framework.core.MarieCore;

/** Client-to-server request for block-hover data at a position. */
@ApiStatus.Internal
public record BlockHoverRequestPayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BlockHoverRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(MarieCore.MOD_ID, "block_hover_request"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, BlockHoverRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(BlockPos.CODEC),
                    BlockHoverRequestPayload::pos,
                    BlockHoverRequestPayload::new
            );

    @Override
    public CustomPacketPayload.Type<BlockHoverRequestPayload> type() {
        return TYPE;
    }
}
