package dev.marie.framework.client;

import dev.marie.framework.client.config.MariesLibClothConfig;
import dev.marie.framework.core.MariesLib;
import dev.marie.framework.core.MariesLibBootstrap;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = MariesLib.MOD_ID, dist = Dist.CLIENT)
public final class MariesLibClient {

    public MariesLibClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> MariesLibClothConfig.create(parent));
        MariesLibBootstrap.setConfigScreenFactory(() -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return MariesLibClothConfig.create(mc.screen);
        });
        MariesLibBootstrap.setExportScreenFactory(parent -> new MariesLibExportScreen((Screen) parent));
        MariesLibBootstrap.setImportScreenFactory(parent -> new MariesLibImportScreen((Screen) parent));
    }
}
