package dev.marie.framework.compat.jade;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Discovered by Jade via its own annotation scan of the mod jar — no META-INF/services entry
 * needed here, unlike the EMI/REI plugins.
 */
@WailaPlugin(MarieCore.MOD_ID)
@ApiStatus.Internal
public final class MarieJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        if (!ModList.get().isLoaded("jade")) {
            return;
        }
        registration.registerBlockDataProvider(BlockHoverJadeServerProvider.INSTANCE, Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (!ModList.get().isLoaded("jade")) {
            return;
        }
        registration.registerBlockComponent(BlockHoverJadeComponentProvider.INSTANCE, Block.class);
    }
}
