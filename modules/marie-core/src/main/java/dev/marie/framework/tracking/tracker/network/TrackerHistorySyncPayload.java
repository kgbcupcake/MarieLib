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
 * Low-frequency server→client tracker sync: completed-period history and open-period state for
 * one or more trackers. {@code data} maps {@code trackerId.toString()} to a compound holding a
 * {@code "history"} list (encoded {@code TrackerHistoryEntry}s) and an optional {@code "period"}
 * (encoded {@code TrackingPeriodState}).
 *
 * <p>Sent as a full snapshot (every registered tracker) on player login, and as a targeted
 * single-tracker resync when {@code TrackerManager} closes a period. See
 * {@code TrackerNetworking#sendFullSnapshot} / {@code #sendPeriodResync}.</p>
 */
@ApiStatus.Internal
public record TrackerHistorySyncPayload(CompoundTag data) implements CustomPacketPayload {

    public static final Type<TrackerHistorySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MarieCore.MOD_ID, "tracker_history_sync"));

    public static final StreamCodec<ByteBuf, TrackerHistorySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, TrackerHistorySyncPayload::data,
            TrackerHistorySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
