package dev.marie.MariesLib.kubejs.bindings;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPI;
import dev.marie.MariesLib.api.ValueDefinition;
import dev.marie.MariesLib.api.ValueSourceTrigger;
import dev.marie.MariesLib.api.registry.ValueRegistry;
import dev.marie.MariesLib.handler.SourceApplicationPipeline;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server script bindings for runtime MarieLib queries and mutations.
 */
@ApiStatus.Experimental
public final class MarieKubeServerBindings {

    private MarieKubeServerBindings() {}

    public static float getValueLevel(Player player, String valueKey) {
        return MarieAPI.getValueLevel(player, valueKey);
    }

    public static void forceSetValue(Player player, String valueKey, float value) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!TrackingAttachment.isRegistered()) {
            return;
        }
        TrackingData data = TrackingAttachment.getData(serverPlayer);
        if (SourceApplicationPipeline.writeDirectValue(serverPlayer, data, valueKey, value)) {
            SourceApplicationPipeline.finalizeDirectWrite(serverPlayer, data);
        }
    }

    public static List<String> getValueKeys() {
        return ValueRegistry.getAll().stream()
                .map(ValueDefinition::getId)
                .toList();
    }

    public static void fireSourceTrigger(Player player, String itemId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ResourceLocation id = ResourceLocation.parse(itemId);
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        MarieAPI.fireSourceTrigger(
                serverPlayer,
                ValueSourceTrigger.itemConsumed(id, 1.0),
                stack);
    }
}
