package dev.marie.framework.tracking;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.core.IMarieConfig;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * NeoForge data attachment for {@link MilestoneProgressData}.
 * <p>
 * Access is gated by {@link FeatureFlagCache#enableMilestones()}: when milestones are disabled,
 * no per-player data is created or read.
 */
@ApiStatus.Internal
public final class MilestoneProgressAttachment {

    public static final String ATTACHMENT_ID = "milestone_progress";

    public static Supplier<AttachmentType<MilestoneProgressData>> MILESTONE_PROGRESS;

    private static boolean registered;

    private MilestoneProgressAttachment() {}

    public static void register(IEventBus modEventBus) {
        if (registered) {
            return;
        }
        registered = true;
        String modId = IMarieConfig.get().modId();
        DeferredRegister<AttachmentType<?>> attachmentTypes =
                DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, modId);
        MILESTONE_PROGRESS = attachmentTypes.register(ATTACHMENT_ID, () ->
                AttachmentType.builder(MilestoneProgressData::createNew)
                        .serialize(MilestoneProgressData.CODEC)
                        .copyOnDeath()
                        .build()
        );
        attachmentTypes.register(modEventBus);
    }

    public static boolean isRegistered() {
        return MILESTONE_PROGRESS != null;
    }

    @Nullable
    public static AttachmentType<MilestoneProgressData> attachmentType() {
        Supplier<AttachmentType<MilestoneProgressData>> supplier = MILESTONE_PROGRESS;
        return supplier != null ? supplier.get() : null;
    }

    /**
     * Returns milestone progress for the player, lazily initializing on first access.
     * Returns {@code null} when milestones are disabled or the attachment is not registered.
     */
    @Nullable
    public static MilestoneProgressData getData(Player player) {
        if (!FeatureFlagCache.enableMilestones()) {
            return null;
        }
        AttachmentType<MilestoneProgressData> type = attachmentType();
        if (type == null) {
            return null;
        }
        return player.getData(type);
    }

    public static void setData(Player player, MilestoneProgressData data) {
        if (!FeatureFlagCache.enableMilestones()) {
            return;
        }
        AttachmentType<MilestoneProgressData> type = attachmentType();
        if (type != null) {
            player.setData(type, data);
        }
    }
}
