package dev.marie.MariesLib.tracking;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MemoryView;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;
import java.util.List;

@ApiStatus.Internal
public final class TrackingAttachment {

    public static Supplier<AttachmentType<TrackingData>> TRACKING;

    private TrackingAttachment() {}

    public static void register(IEventBus modEventBus) {
        String modId = MarieLibContext.get().modId();
        DeferredRegister<AttachmentType<?>> attachmentTypes =
                DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, modId);
        TRACKING = attachmentTypes.register("tracking", () ->
                AttachmentType.builder(TrackingData::createNew)
                        .serialize(TrackingData.CODEC)
                        .build()
        );
        attachmentTypes.register(modEventBus);
    }

    private static String trackingAttachmentNbtPrefix() {
        return "neoforge:attachments." + MarieLibContext.get().modId() + ":tracking";
    }

    public static String getValueNbtPath(String valueKey) {
        return trackingAttachmentNbtPrefix() + ".values." + valueKey;
    }

    public static String getTotalNbtPath() {
        return trackingAttachmentNbtPrefix() + ".total";
    }

    public static void logAllValueNbtPaths() {
        List<String> valueKeys = MarieLibContext.get().valueKeys();
        for (String valueKey : valueKeys) {
            MariesLib.LOGGER.info("[MarieLib] Value NBT path: {}", getValueNbtPath(valueKey));
        }
    }

    public static float getTotal(Player player) {
        TrackingData data = player.getData(TRACKING.get());
        return data.total;
    }

    public static float getValueLevel(Player player, String valueKey) {
        TrackingData data = player.getData(TRACKING.get());
        Float value = data.values.get(valueKey);
        return value != null ? value : -1.0f;
    }

    public static MemoryView getSourceMemoryView(Player player) {
        TrackingData data = player.getData(TRACKING.get());
        return new TrackingDataMemoryView(data);
    }
}
