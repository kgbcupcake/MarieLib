package dev.marie.framework.network;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import dev.marie.framework.core.MarieCore;

/**
 * Server-to-client response for block-hover data. {@code found = false} with an empty tag is
 * the explicit "no provider matched" case — the request is never silently dropped.
 */
@ApiStatus.Internal
public record BlockHoverResponsePayload(BlockPos pos, boolean found, CompoundTag data) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BlockHoverResponsePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(MarieCore.MOD_ID, "block_hover_response"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, BlockHoverResponsePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(BlockPos.CODEC),
                    BlockHoverResponsePayload::pos,
                    ByteBufCodecs.BOOL,
                    BlockHoverResponsePayload::found,
                    ByteBufCodecs.fromCodec(CompoundTag.CODEC),
                    BlockHoverResponsePayload::data,
                    BlockHoverResponsePayload::new
            );

    @Override
    public CustomPacketPayload.Type<BlockHoverResponsePayload> type() {
        return TYPE;
    }
}
