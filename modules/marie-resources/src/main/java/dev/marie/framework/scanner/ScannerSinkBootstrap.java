package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Registers marie-resources's report writers with marie-core's sink registries at mod
 * setup. Mod initialization is itself event-driven in NeoForge, so {@link EventBusSubscriber}
 * (targeting the mod bus) is the natural hook here — core never has to name these writer
 * classes directly, per the locked one-directional module graph.
 */
@ApiStatus.Internal
@EventBusSubscriber(modid = MarieCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ScannerSinkBootstrap {

    private ScannerSinkBootstrap() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        ScanReportSinkRegistry.register(ScanReportWriter.INSTANCE);
        TagRecommendationSinkRegistry.register(TagRecommendationWriter.INSTANCE);
    }
}
