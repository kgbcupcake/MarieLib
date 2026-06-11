package dev.marie.MariesLib.client;

import dev.marie.MariesLib.client.config.MariesLibClothConfig;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.core.MariesLibBootstrap;
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
        MariesLibBootstrap.setExportScreenFactory(MariesLibExportScreen::new);
        MariesLibBootstrap.setImportScreenFactory(MariesLibImportScreen::new);
    }
}
