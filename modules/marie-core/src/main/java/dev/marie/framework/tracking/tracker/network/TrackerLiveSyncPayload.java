package dev.marie.framework.tracking.tracker.network;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * High-frequency server→client tracker sync: the current-period accumulator value for every
 * tracker that changed for one player since their last sync. {@code data} maps
 * {@code trackerId.toString()} to a float tag holding the current value.
 *
 * <p>Sent throttled (at most once per {@code IMarieConfig#trackerSyncIntervalTicks()}) and only
 * for dirty trackers — not a full snapshot. See {@code TrackerNetworking#sendLiveValues}.</p>
 */
@ApiStatus.Internal
public record TrackerLiveSyncPayload(CompoundTag data) implements CustomPacketPayload {

    public static final Type<TrackerLiveSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MarieCore.MOD_ID, "tracker_live_sync"));

    public static final StreamCodec<ByteBuf, TrackerLiveSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, TrackerLiveSyncPayload::data,
            TrackerLiveSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
