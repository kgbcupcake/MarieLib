package dev.marie.framework.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marieapi.MarieAPI;
import dev.marie.framework.api.value.ValueSourceTrigger;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.core.MarieCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Fires generic {@link ValueSourceTrigger.TriggerType#BLOCK_BROKEN} and
 * {@link ValueSourceTrigger.TriggerType#ENTITY_KILLED} triggers from ordinary NeoForge events.
 * MarieLib has no opinion on what these triggers mean — consuming mods decide.
 */
@ApiStatus.Internal
public final class GameplayTriggerListener {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (FeatureFlagCache.enableDebugLogging()) {
            MarieCore.LOGGER.debug("[GameplayTriggerListener] BLOCK_BROKEN fired: {}",
                    "player=" + serverPlayer.getName().getString() + " block=" + blockId + " pos=" + event.getPos());
        }
        MarieAPI.fireSourceTrigger(serverPlayer, ValueSourceTrigger.blockBroken(blockId), ItemStack.EMPTY);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        LivingEntity killed = event.getEntity();
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
        if (FeatureFlagCache.enableDebugLogging()) {
            MarieCore.LOGGER.debug("[GameplayTriggerListener] ENTITY_KILLED fired: {}",
                    "killer=" + killer.getName().getString() + " entity=" + entityId);
        }
        MarieAPI.fireSourceTrigger(killer, ValueSourceTrigger.entityKilled(entityId), ItemStack.EMPTY);
    }
}
