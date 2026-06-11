package dev.marie.MariesLib.client;

import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLibBootstrap;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client-only casts for screen factories registered on {@link MarieLibContext}
 * or {@link MariesLibBootstrap}.
 */
public final class ClientScreenFactories {

    private ClientScreenFactories() {}

    public static Screen getConfigScreen() {
        Object screen = MarieLibContext.isRegistered()
                ? MarieLibContext.get().configScreenFactory()
                : MariesLibBootstrap.getConfigScreenFactory().get();
        return (Screen) screen;
    }

    public static Screen exportScreen(Screen parent) {
        Object screen = MarieLibContext.isRegistered()
                ? MarieLibContext.get().exportScreenFactory(parent)
                : MariesLibBootstrap.getExportScreenFactory().apply(parent);
        return (Screen) screen;
    }

    public static Screen importScreen(Screen parent) {
        Object screen = MarieLibContext.isRegistered()
                ? MarieLibContext.get().importScreenFactory(parent)
                : MariesLibBootstrap.getImportScreenFactory().apply(parent);
        return (Screen) screen;
    }
}
