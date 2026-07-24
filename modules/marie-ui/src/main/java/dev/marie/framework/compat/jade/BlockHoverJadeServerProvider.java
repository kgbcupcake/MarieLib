package dev.marie.framework.compat.jade;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.hover.BlockHoverProvider;
import dev.marie.framework.api.registry.BlockHoverProviderRegistry;
import dev.marie.framework.core.MarieCore;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Server-side half of the Jade integration: independent of {@code BlockHoverSyncChannel}, which
 * exists only for the custom overlay fallback — Jade has its own client/server sync mechanism.
 */
@ApiStatus.Internal
public enum BlockHoverJadeServerProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        Level level = accessor.getLevel();
        Player player = accessor.getPlayer();
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockPos pos = accessor.getPosition();
        BlockState state = accessor.getBlockState();
        for (BlockHoverProvider provider : BlockHoverProviderRegistry.getAll()) {
            if (provider.supports(level, pos, state)) {
                data.merge(provider.computeServerData(serverLevel, pos, serverPlayer));
                return;
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(MarieCore.MOD_ID, "block_hover_data");
    }
}
