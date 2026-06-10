package dev.marie.MariesLib.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.config.ModCompatRegistry;
import dev.marie.MariesLib.util.MarieItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.UUID;

@ApiStatus.Internal
public class SourceEatingHandler {

    private static final int SOURCE_ONLY_EAT_COOLDOWN_TICKS = 20;
    private static final HashMap<UUID, Long> LAST_SOURCE_ONLY_EAT_GAME_TIME = new HashMap<>();
    private static final HashMap<UUID, PendingSourceOnlyFinish> PENDING_SOURCE_ONLY_FINISH = new HashMap<>();

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!ModuleCache.enableDecay || !ModuleCache.enableSourceApplication) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        Long lastEat = LAST_SOURCE_ONLY_EAT_GAME_TIME.get(player.getUUID());
        if (lastEat != null && now - lastEat < SOURCE_ONLY_EAT_COOLDOWN_TICKS) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        FoodProperties source = stack.getItem().getFoodProperties(stack, player);
        if (source == null || source.canAlwaysEat()) return;
        if (player.canEat(false)) return;
        if (shouldBlockSourceOnlyAtFullHunger(stack, source)) {
            event.setCanceled(true);
            return;
        }
        ItemStack captured = stack.copy();
        InteractionHand hand = event.getHand();
        event.setCanceled(true);
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            server.execute(() -> performSourceOnlyConsume(player, captured, hand));
        }
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PendingSourceOnlyFinish pending = PENDING_SOURCE_ONLY_FINISH.get(player.getUUID());
        if (pending == null) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!pending.matches(stack)) {
            PENDING_SOURCE_ONLY_FINISH.remove(player.getUUID());
            return;
        }
        FoodProperties source = stack.getItem().getFoodProperties(stack, player);
        if (source == null) {
            PENDING_SOURCE_ONLY_FINISH.remove(player.getUUID());
            return;
        }
        PENDING_SOURCE_ONLY_FINISH.remove(player.getUUID());
        TrackingData tracking = player.getData(TrackingAttachment.TRACKING.get());
        long gameTimeMs = player.level().getGameTime() * 50L;
        tracking.tickTime(gameTimeMs);
        tracking.tick();
        SourceApplicationPipeline.process(player, stack, tracking, gameTimeMs);
        LAST_SOURCE_ONLY_EAT_GAME_TIME.put(player.getUUID(), player.level().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PENDING_SOURCE_ONLY_FINISH.remove(player.getUUID());
    }

    public static boolean isSourceOnlyPipelinePending(ServerPlayer player) {
        return PENDING_SOURCE_ONLY_FINISH.containsKey(player.getUUID());
    }

    public static boolean isSourceOnlyPipelinePending(ServerPlayer player, ItemStack stack) {
        PendingSourceOnlyFinish pending = PENDING_SOURCE_ONLY_FINISH.get(player.getUUID());
        return pending != null && pending.matches(stack);
    }

    private static boolean shouldBlockSourceOnlyAtFullHunger(ItemStack stack, FoodProperties source) {
        if (ModuleCache.enableBlockHeavySources) {
            if (ModCompatRegistry.isLoaded("solonion")) {
                int threshold = ModCompatRegistry.getHeavySourceThreshold();
                if (source != null && source.nutrition() >= threshold) {
                    return true;
                }
            } else if (stack.is(MarieItemTags.heavySource())) {
                return true;
            }
        }
        return ModuleCache.enableBlockLightSource && stack.is(MarieItemTags.lightSource());
    }

    private static boolean performSourceOnlyConsume(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        PENDING_SOURCE_ONLY_FINISH.remove(player.getUUID());
        if (!ModuleCache.enableDecay || !ModuleCache.enableSourceApplication) {
            return false;
        }
        if (!player.isAlive()) {
            return false;
        }
        FoodProperties sourceNow = stack.getItem().getFoodProperties(stack, player);
        if (sourceNow == null || sourceNow.canAlwaysEat()) {
            return false;
        }
        if (shouldBlockSourceOnlyAtFullHunger(stack, sourceNow)) {
            return false;
        }

        ItemStack live = hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
        if (live.isEmpty() || !ItemStack.isSameItemSameComponents(stack, live)) {
            return false;
        }

        if (player.isUsingItem()) {
            return false;
        }

        player.startUsingItem(hand);
        if (!player.isUsingItem()) {
            return false;
        }
        PENDING_SOURCE_ONLY_FINISH.put(player.getUUID(), new PendingSourceOnlyFinish(stack.copy()));
        return true;
    }

    private record PendingSourceOnlyFinish(ItemStack stack) {
        private boolean matches(ItemStack candidate) {
            return !candidate.isEmpty() && ItemStack.isSameItemSameComponents(stack, candidate);
        }
    }
}
