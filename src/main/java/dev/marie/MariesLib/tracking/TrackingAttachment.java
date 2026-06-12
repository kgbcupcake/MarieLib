package dev.marie.MariesLib.tracking;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ApplicationHistoryView;
import dev.marie.MariesLib.api.impl.EmptyApplicationHistoryView;
import dev.marie.MariesLib.core.IMarieLibConfig;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

@ApiStatus.Internal
public final class TrackingAttachment {

    public static final String ATTACHMENT_ID = "tracking";
    public static final String NBT_ATTACHMENTS_PREFIX = "neoforge:attachments.";
    public static final String NBT_VALUES_SEGMENT = ".values.";

    public static Supplier<AttachmentType<TrackingData>> TRACKING;

    private static boolean registered;

    private TrackingAttachment() {}

    public static void register(IEventBus modEventBus) {
        if (registered) {
            return;
        }
        registered = true;
        String modId = IMarieLibConfig.get().modId();
        DeferredRegister<AttachmentType<?>> attachmentTypes =
                DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, modId);
        TRACKING = attachmentTypes.register(ATTACHMENT_ID, () ->
                AttachmentType.builder(TrackingData::createNew)
                        .serialize(TrackingData.CODEC)
                        .build()
        );
        attachmentTypes.register(modEventBus);
    }

    public static boolean isRegistered() {
        return TRACKING != null;
    }

    @Nullable
    public static AttachmentType<TrackingData> attachmentType() {
        Supplier<AttachmentType<TrackingData>> supplier = TRACKING;
        return supplier != null ? supplier.get() : null;
    }

    public static TrackingData getData(Player player) {
        AttachmentType<TrackingData> type = attachmentType();
        if (type == null) {
            return TrackingData.createNew();
        }
        return player.getData(type);
    }

    public static void setData(Player player, TrackingData data) {
        AttachmentType<TrackingData> type = attachmentType();
        if (type != null) {
            player.setData(type, data);
        }
    }

    private static String trackingAttachmentNbtPrefix() {
        String modId = IMarieLibConfig.get().modId();
        return NBT_ATTACHMENTS_PREFIX + modId + ":" + ATTACHMENT_ID;
    }

    public static String getValueNbtPath(String valueKey) {
        return trackingAttachmentNbtPrefix() + NBT_VALUES_SEGMENT + valueKey;
    }

    public static String getTotalNbtPath() {
        return trackingAttachmentNbtPrefix() + ".total";
    }

    public static void logAllValueNbtPaths() {
        if (!MarieLibContext.isRegistered()) {
            return;
        }
        List<String> valueKeys = MarieLibContext.get().valueKeys();
        for (String valueKey : valueKeys) {
            MariesLib.LOGGER.info("[MarieLib] Value NBT path: {}", getValueNbtPath(valueKey));
        }
    }

    public static float getTotal(Player player) {
        AttachmentType<TrackingData> type = attachmentType();
        if (type == null) {
            return 0f;
        }
        TrackingData data = player.getData(type);
        return data.total;
    }

    public static float getValueLevel(Player player, String valueKey) {
        AttachmentType<TrackingData> type = attachmentType();
        if (type == null) {
            return -1.0f;
        }
        TrackingData data = player.getData(type);
        Float value = data.values.get(valueKey);
        return value != null ? value : -1.0f;
    }

    public static ApplicationHistoryView getApplicationHistoryView(Player player) {
        AttachmentType<TrackingData> type = attachmentType();
        if (type == null) {
            return EmptyApplicationHistoryView.INSTANCE;
        }
        TrackingData data = player.getData(type);
        return new TrackingDataApplicationHistoryView(data);
    }
}
