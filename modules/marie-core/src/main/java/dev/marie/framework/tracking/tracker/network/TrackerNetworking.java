package dev.marie.framework.tracking.tracker.network;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.ClientTrackerCache;
import dev.marie.framework.tracking.tracker.definition.TrackerDefinition;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.marie.framework.tracking.tracker.definition.TrackingPeriodState;
import dev.marie.framework.tracking.tracker.registry.TrackerRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Registers the tracker sync packet channel and dispatches inbound payloads to
 * {@link ClientTrackerCache} on the client. Server-side sending helpers back
 * {@link dev.marie.framework.tracking.tracker.TrackerManager}'s dirty-sweep (live values) and
 * {@link dev.marie.framework.handler.PlayerTrackingLifecycle}/period-completion (history + period
 * state).
 *
 * <p>Internal — consuming mods never call this directly, they read tracker state via
 * {@link dev.marie.framework.tracking.tracker.MarieTracking}.</p>
 */
@ApiStatus.Internal
public final class TrackerNetworking {

    private TrackerNetworking() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(TrackerNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MarieCore.MOD_ID).versioned("1");
        registrar.playToClient(
                TrackerLiveSyncPayload.TYPE,
                TrackerLiveSyncPayload.STREAM_CODEC,
                TrackerNetworking::handleLiveSync);
        registrar.playToClient(
                TrackerHistorySyncPayload.TYPE,
                TrackerHistorySyncPayload.STREAM_CODEC,
                TrackerNetworking::handleHistorySync);
    }

    private static void handleLiveSync(TrackerLiveSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            CompoundTag data = payload.data();
            for (String key : data.getAllKeys()) {
                ResourceLocation trackerId = ResourceLocation.parse(key);
                ClientTrackerCache.setCurrentValue(trackerId, data.getFloat(key));
            }
        });
    }

    private static void handleHistorySync(TrackerHistorySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            CompoundTag outer = payload.data();
            for (String key : outer.getAllKeys()) {
                ResourceLocation trackerId = ResourceLocation.parse(key);
                CompoundTag entry = outer.getCompound(key);
                List<TrackerHistoryEntry> history = new ArrayList<>();
                if (entry.contains("history")) {
                    ListTag list = entry.getList("history", Tag.TAG_COMPOUND);
                    for (Tag t : list) {
                        TrackerHistoryEntry.CODEC.parse(NbtOps.INSTANCE, t).result().ifPresent(history::add);
                    }
                }
                ClientTrackerCache.setHistory(trackerId, List.copyOf(history));
                if (entry.contains("period")) {
                    TrackingPeriodState.CODEC.parse(NbtOps.INSTANCE, entry.getCompound("period"))
                            .result()
                            .ifPresent(state -> ClientTrackerCache.setPeriodState(trackerId, state));
                }
                // The snapshot carries the live current-period accumulator alongside history/period
                // so a freshly-logged-in client shows the right in-progress value without waiting
                // for the first dirty-sweep live sync. Absent key -> getFloat returns 0f.
                ClientTrackerCache.setCurrentValue(trackerId, entry.getFloat("current"));
            }
        });
    }

    /** Sends only the given trackers' current accumulator values — the high-frequency dirty sweep. */
    public static void sendLiveValues(ServerPlayer player, Set<ResourceLocation> trackerIds, TrackingData tracking) {
        if (trackerIds.isEmpty()) {
            return;
        }
        CompoundTag data = new CompoundTag();
        for (ResourceLocation trackerId : trackerIds) {
            data.putFloat(trackerId.toString(), tracking.trackingAccumulators.getOrDefault(trackerId, 0f));
        }
        PacketDistributor.sendToPlayer(player, new TrackerLiveSyncPayload(data));
    }

    /** Sends every registered tracker's history/period-state — the on-login full snapshot. */
    public static void sendFullSnapshot(ServerPlayer player, TrackingData tracking) {
        CompoundTag outer = new CompoundTag();
        for (TrackerDefinition definition : TrackerRegistry.getAll()) {
            ResourceLocation trackerId = definition.getId();
            CompoundTag entry = buildTrackerEntry(trackerId, tracking);
            if (entry != null) {
                outer.put(trackerId.toString(), entry);
            }
        }
        if (!outer.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new TrackerHistorySyncPayload(outer));
        }
    }

    /** Sends just one tracker's history/period-state — the on-period-completion targeted resync. */
    public static void sendPeriodResync(ServerPlayer player, ResourceLocation trackerId, TrackingData tracking) {
        CompoundTag entry = buildTrackerEntry(trackerId, tracking);
        if (entry == null) {
            return;
        }
        CompoundTag outer = new CompoundTag();
        outer.put(trackerId.toString(), entry);
        PacketDistributor.sendToPlayer(player, new TrackerHistorySyncPayload(outer));
    }

    // Package-private for direct unit coverage — see TrackerNetworkingBuildEntryTest.
    static CompoundTag buildTrackerEntry(ResourceLocation trackerId, TrackingData tracking) {
        List<TrackerHistoryEntry> history = tracking.trackingHistory.get(trackerId);
        TrackingPeriodState period = tracking.trackerPeriodStates.get(trackerId);
        float current = tracking.trackingAccumulators.getOrDefault(trackerId, 0f);
        if ((history == null || history.isEmpty()) && period == null && current == 0f) {
            return null;
        }
        CompoundTag entry = new CompoundTag();
        ListTag historyList = new ListTag();
        if (history != null) {
            for (TrackerHistoryEntry historyEntry : history) {
                TrackerHistoryEntry.CODEC.encodeStart(NbtOps.INSTANCE, historyEntry)
                        .result()
                        .ifPresent(historyList::add);
            }
        }
        entry.put("history", historyList);
        if (period != null) {
            TrackingPeriodState.CODEC.encodeStart(NbtOps.INSTANCE, period)
                    .result()
                    .ifPresent(periodTag -> entry.put("period", periodTag));
        }
        entry.putFloat("current", current);
        return entry;
    }
}
