package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@ApiStatus.Internal
public class SourceAppliedHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSourceApplied(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (SourceEatingHandler.isSourceOnlyPipelinePending(player, stack)) {
            return;
        }
        FoodProperties sourceProps = stack.getItem().components().get(DataComponents.FOOD);
        if (sourceProps == null) {
            return;
        }

        TrackingData tracking = player.getData(TrackingAttachment.TRACKING.get());
        long gameTimeMs = player.level().getGameTime() * 50L;
        tracking.tickTime(gameTimeMs);
        tracking.tick();

        SourceApplicationPipeline.process(player, stack, tracking, gameTimeMs);
    }
}
