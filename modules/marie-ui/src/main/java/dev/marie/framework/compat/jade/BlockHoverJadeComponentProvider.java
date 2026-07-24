package dev.marie.framework.compat.jade;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.hover.BlockHoverProvider;
import dev.marie.framework.api.registry.BlockHoverProviderRegistry;
import dev.marie.framework.core.MarieCore;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Client-side half of the Jade integration; see {@link BlockHoverJadeServerProvider}. */
@ApiStatus.Internal
public enum BlockHoverJadeComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Level level = accessor.getLevel();
        BlockPos pos = accessor.getPosition();
        BlockState state = accessor.getBlockState();

        // Re-run the same client-safe supports() check the server used, rather than trusting a
        // found flag — supports() only ever looks at block/blockstate identity, so it's
        // deterministic on both sides.
        for (BlockHoverProvider provider : BlockHoverProviderRegistry.getAll()) {
            if (provider.supports(level, pos, state)) {
                tooltip.addAll(provider.renderLines(accessor.getServerData(), level, pos));
                return;
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(MarieCore.MOD_ID, "block_hover_component");
    }
}
