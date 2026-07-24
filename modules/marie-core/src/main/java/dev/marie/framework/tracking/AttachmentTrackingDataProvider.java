package dev.marie.framework.tracking;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.reporting.ApplicationHistoryView;
import dev.marie.framework.core.MarieDataProvider;
import dev.marie.framework.handler.SourceApplicationPipeline;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

@ApiStatus.Internal
public final class AttachmentTrackingDataProvider implements MarieDataProvider {

    @Override
    public float getAggregateLevel(Player player) {
        return TrackingAttachment.getTotal(player);
    }

    @Override
    public float getValueLevel(Player player, String valueKey) {
        return TrackingAttachment.getValueLevel(player, valueKey);
    }

    @Override
    public ApplicationHistoryView getApplicationHistoryView(Player player) {
        return TrackingAttachment.getApplicationHistoryView(player);
    }

    @Override
    public void modifyValue(Player player, String valueKey, float delta) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!TrackingAttachment.isRegistered()) {
            return;
        }
        TrackingData data = TrackingAttachment.getData(serverPlayer);
        if (SourceApplicationPipeline.applyDirectDelta(serverPlayer, data, valueKey, delta)) {
            SourceApplicationPipeline.finalizeDirectWrite(serverPlayer, data);
        }
    }
}
