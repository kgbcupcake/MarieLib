package dev.marie.framework.compat.jei;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MariesLib;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
@ApiStatus.Internal
public final class MarieJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MariesLib.MOD_ID, "jei_plugin");
    }
}
