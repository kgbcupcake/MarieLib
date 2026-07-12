package dev.marie.framework.network;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.BlockHoverProvider;
import dev.marie.framework.api.registry.BlockHoverProviderRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-side handler for {@link BlockHoverRequestPayload}. */
@ApiStatus.Internal
public final class BlockHoverRequestHandler {

    private BlockHoverRequestHandler() {}

    public static void handle(BlockHoverRequestPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        context.enqueueWork(() -> handleOnServerThread(payload, serverPlayer, level));
    }

    private static void handleOnServerThread(BlockHoverRequestPayload payload, ServerPlayer player, ServerLevel level) {
        BlockPos pos = payload.pos();
        double range = player.blockInteractionRange();
        double distSq = Vec3.atCenterOf(pos).distanceToSqr(player.getEyePosition());
        if (distSq > range * range) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        for (BlockHoverProvider provider : BlockHoverProviderRegistry.getAll()) {
            if (provider.supports(level, pos, state)) {
                CompoundTag data = provider.computeServerData(level, pos, player);
                PacketDistributor.sendToPlayer(player, new BlockHoverResponsePayload(pos, true, data));
                return;
            }
        }
        PacketDistributor.sendToPlayer(player, new BlockHoverResponsePayload(pos, false, new CompoundTag()));
    }
}
