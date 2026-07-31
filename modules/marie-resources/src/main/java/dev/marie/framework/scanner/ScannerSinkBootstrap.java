package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.registry.RegistryLifecycleManager;
import dev.marie.framework.resources.tags.InstanceTagRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Registers marie-resources's report writers and registry implementations with marie-core's
 * SPI registries at mod setup. Mod initialization is itself event-driven in NeoForge, so
 * {@link EventBusSubscriber} (targeting the mod bus) is the natural hook here — core never has
 * to name these marie-resources classes directly, per the locked one-directional module graph.
 */
@ApiStatus.Internal
@EventBusSubscriber(modid = MarieCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ScannerSinkBootstrap {

    private ScannerSinkBootstrap() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        ScanReportSinkRegistry.register(ScanReportWriter.INSTANCE);
        TagRecommendationSinkRegistry.register(TagRecommendationWriter.INSTANCE);
        InstanceTagSourceRegistry.register(InstanceTagRegistry.INSTANCE);
        RegistryLifecycleManager.registerRegistry("InstanceTagRegistry",
                InstanceTagRegistry::load, InstanceTagRegistry::reload, resourceManager -> {});
    }
}
