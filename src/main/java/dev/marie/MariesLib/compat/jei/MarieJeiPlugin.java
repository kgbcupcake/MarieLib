package dev.marie.MariesLib.compat.jei;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.MariesLib;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
@ApiStatus.Internal
public final class MarieJeiPlugin implements IModPlugin {

    public static void bootstrap() {
        // Intentionally empty. Called reflectively from client init to keep optional class loading safe.
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MariesLib.MOD_ID, "jei_plugin");
    }
}
