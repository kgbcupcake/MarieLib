package dev.marie.framework.client;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.BlockHoverProvider;
import dev.marie.framework.api.registry.BlockHoverProviderRegistry;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.network.BlockHoverRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Watches the vanilla crosshair raytrace each client tick and requests block-hover data for
 * whatever block it lands on, filtered client-side by {@link BlockHoverProvider#supports}
 * so ordinary blocks never generate network traffic.
 */
@ApiStatus.Internal
@EventBusSubscriber(modid = MarieCore.MOD_ID, value = Dist.CLIENT)
public final class BlockHoverTracker {

    private static final int REFRESH_INTERVAL_TICKS = 20;

    private static int ticksSinceRequest;

    private BlockHoverTracker() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        HitResult hit = mc.hitResult;
        if (level == null || !(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            BlockHoverClientCache.clear();
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        BlockHoverProvider matched = null;
        for (BlockHoverProvider provider : BlockHoverProviderRegistry.getAll()) {
            if (provider.supports(level, pos, state)) {
                matched = provider;
                break;
            }
        }
        if (matched == null) {
            BlockHoverClientCache.clear();
            return;
        }

        if (!pos.equals(BlockHoverClientCache.getTrackedPos())) {
            BlockHoverClientCache.setTrackedPos(pos, matched);
            sendRequest(pos);
            return;
        }

        ticksSinceRequest++;
        if (ticksSinceRequest >= REFRESH_INTERVAL_TICKS) {
            sendRequest(pos);
        }
    }

    private static void sendRequest(BlockPos pos) {
        ticksSinceRequest = 0;
        PacketDistributor.sendToServer(new BlockHoverRequestPayload(pos));
    }
}
