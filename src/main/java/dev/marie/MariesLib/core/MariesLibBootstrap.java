package dev.marie.MariesLib.core;

import java.util.function.Function;
import java.util.function.Supplier;

import dev.marie.MariesLib.color.ColorRegistry;
import dev.marie.MariesLib.command.MarieCommand;
import dev.marie.MariesLib.config.MariesLibConfigBridge;
import dev.marie.MariesLib.config.MariesLibConfigHolder;
import dev.marie.MariesLib.config.MariesLibConfigIO;
import dev.marie.MariesLib.config.ModCompatRegistry;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.config.PresetRegistry;
import dev.marie.MariesLib.config.LockRegistry;
import dev.marie.MariesLib.handler.RecipeServerHandler;
import dev.marie.MariesLib.handler.ReloadHandler;
import dev.marie.MariesLib.handler.SleepBonusHandler;
import dev.marie.MariesLib.handler.TrackingPlayerEvents;
import dev.marie.MariesLib.handler.ValueDecayHandler;
import dev.marie.MariesLib.handler.ValueEffectsHandler;
import dev.marie.MariesLib.api.ISourceTriggerHandler;
import dev.marie.MariesLib.registry.MarieApiRegistries;
import dev.marie.MariesLib.runtime.TriggerHandlerRegistry;
import dev.marie.MariesLib.registry.MarieAttributes;
import dev.marie.MariesLib.registry.RegistryLifecycleManager;
import dev.marie.MariesLib.runtime.SourceOverrideRegistry;
import dev.marie.MariesLib.runtime.SourceValueRegistry;
import dev.marie.MariesLib.scanner.ScannerSpecRegistry;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bootstraps MariesLib-owned context, registries, and handlers when no Marie mod has
 * registered {@link MarieLibContext} first. Library config always lives in {@code config/marieslib.cfg}.
 */
public final class MariesLibBootstrap {

    private static volatile Supplier<Screen> configScreenFactory = () -> null;
    private static volatile Function<Screen, Screen> exportScreenFactory = parent -> null;
    private static volatile Function<Screen, Screen> importScreenFactory = parent -> null;

    private MariesLibBootstrap() {}

    public static void setConfigScreenFactory(Supplier<Screen> factory) {
        configScreenFactory = factory != null ? factory : () -> null;
    }

    public static void setExportScreenFactory(Function<Screen, Screen> factory) {
        exportScreenFactory = factory != null ? factory : parent -> null;
    }

    public static void setImportScreenFactory(Function<Screen, Screen> factory) {
        importScreenFactory = factory != null ? factory : parent -> null;
    }

    public static void bootstrap(IEventBus modEventBus) {
        MarieLibContext.register(buildContext());
        registerRegistries();
        ModuleCache.refresh();
        registerHandlers(modEventBus);
        RegistryLifecycleManager.loadAll();
        ModCompatRegistry.load();
        MariesLib.LOGGER.info("[MariesLib] Bootstrap complete with owned config");
    }

    private static MarieLibContext buildContext() {
        MariesLibConfigHolder h = MariesLibConfigHolder.get();
        return MarieLibContext.builder(MariesLib.MOD_ID)
                .scannerConfidenceSpreadThreshold(() -> h.scannerConfidenceSpreadThreshold)
                .compositeRatioThreshold(() -> h.compositeRatioThreshold)
                .scannerEnableRecipeInheritance(() -> h.scannerEnableRecipeInheritance)
                .multiValueInheritanceThreshold(() -> h.multiValueInheritanceThreshold)
                .enableDebugLogging(() -> h.enableDebugLogging)
                .memoryWindowMinutes(() -> h.memoryWindowMinutes)
                .memoryWindowCount(() -> h.memoryWindowCount)
                .streakWindowMs(() -> h.streakWindowMs)
                .streakWeight(() -> h.streakWeight)
                .debtThreshold(() -> h.debtThreshold)
                .debtDecayRate(() -> h.debtDecayRate)
                .diminishingSteepness(() -> h.diminishingSteepness)
                .diminishingMidpoint(() -> h.diminishingMidpoint)
                .debugMemoryLogging(() -> h.debugMemoryLogging)
                .excessThreshold(() -> h.excessThreshold)
                .lowThreshold(() -> h.lowThreshold)
                .criticalThreshold(() -> h.criticalThreshold)
                .decayIntervalTicks(() -> h.decayIntervalTicks)
                .valueDecayRateProvider(key -> h.defaultDecayRate)
                .criticalThresholdProvider(key -> h.criticalThreshold)
                .showJoinMessage(() -> h.showJoinMessage)
                .trackingMemoryConfigProvider(h::toTrackingMemoryConfig)
                .clientMemoryConfigProvider(h::toTrackingMemoryConfig)
                .configScreenFactory(() -> configScreenFactory.get())
                .exportScreenFactory(exportScreenFactory)
                .importScreenFactory(importScreenFactory)
                .configExporter(MariesLibConfigBridge::buildExportRoot)
                .configImporter(MariesLibConfigBridge::applyImport)
                .currentConfigPresetValues(() -> MariesLibConfigHolder.get().toPresetValues())
                .applyPresetValues(v -> {
                    MariesLibConfigHolder.get().applyPresetValues(v);
                    MariesLibConfigIO.save();
                })
                .build();
    }

    private static void registerRegistries() {
        RegistryLifecycleManager.registerRegistry("LockRegistry", LockRegistry::load, LockRegistry::reload,
                LockRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry("ColorRegistry", ColorRegistry::load, ColorRegistry::reload,
                ColorRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry("ScannerSpecRegistry", ScannerSpecRegistry::load,
                ScannerSpecRegistry::reload, ScannerSpecRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry("SourceOverrideRegistry", SourceOverrideRegistry::load,
                SourceOverrideRegistry::reload, SourceOverrideRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry("SourceValueRegistry", SourceValueRegistry::load,
                SourceValueRegistry::reload, SourceValueRegistry::loadFromDatapack);
        RegistryLifecycleManager.registerRegistry("PresetRegistry", PresetRegistry::reload, PresetRegistry::reload);
    }

    private static boolean handlersRegistered;

    private static void registerHandlers(IEventBus modEventBus) {
        if (handlersRegistered) {
            return;
        }
        handlersRegistered = true;
        NeoForge.EVENT_BUS.register(new ValueDecayHandler());
        NeoForge.EVENT_BUS.register(new ValueEffectsHandler());
        NeoForge.EVENT_BUS.register(new SleepBonusHandler());
        NeoForge.EVENT_BUS.register(new TrackingPlayerEvents());
        NeoForge.EVENT_BUS.register(new ReloadHandler());
        NeoForge.EVENT_BUS.register(new RecipeServerHandler());
        NeoForge.EVENT_BUS.register(new MarieCommand());
        MarieApiRegistries.freezeModOnlyRegistriesAfterCommonSetup();
        for (ISourceTriggerHandler handler : TriggerHandlerRegistry.getAll()) {
            handler.register(NeoForge.EVENT_BUS);
        }
        MarieAttributes.register(modEventBus);
        TrackingAttachment.register(modEventBus);
    }
}
