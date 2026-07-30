package dev.marie.framework.ui;

import com.mojang.logging.LogUtils;

import dev.marie.framework.api.ApiStatus;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lets a consuming mod detect when a screen backed by a specific
 * {@link net.minecraft.world.inventory.MenuType} opens, without MarieLib ever importing that
 * mod's screen or menu class.
 *
 * <p>Matching is purely by the menu type's registry name, read back off the opened menu via
 * {@link BuiltInRegistries#MENU}. This is the whole point of the class: it stays generic and
 * dependency-free by never referencing any foreign mod's types.</p>
 *
 * <p>Register interest during your mod's client init:</p>
 * <pre>{@code
 * ForeignScreenDetector.registerInterest(
 *     ResourceLocation.fromNamespaceAndPath("othermod", "some_menu"),
 *     screen -> { ... });
 * }</pre>
 */
@ApiStatus.Experimental
public final class ForeignScreenDetector {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, List<Consumer<Screen>>> LISTENERS = new ConcurrentHashMap<>();
    private static volatile boolean subscribed;

    private ForeignScreenDetector() {}

    /**
     * Registers interest in screens backed by the given menu type.
     *
     * <p>The callback receives the opened {@link Screen} itself. Consuming mods that need the
     * underlying {@link AbstractContainerMenu} can cast the screen to {@link MenuAccess} —
     * MarieLib makes no assumption about the screen's concrete type.</p>
     *
     * @param menuTypeId the registry name of the {@link net.minecraft.world.inventory.MenuType} to watch for
     * @param callback   invoked on the client whenever a matching screen opens
     */
    public static void registerInterest(ResourceLocation menuTypeId, Consumer<Screen> callback) {
        Objects.requireNonNull(menuTypeId, "menuTypeId");
        Objects.requireNonNull(callback, "callback");
        ensureSubscribed();
        LISTENERS.computeIfAbsent(menuTypeId, id -> new CopyOnWriteArrayList<>()).add(callback);
    }

    private static void ensureSubscribed() {
        if (subscribed) {
            return;
        }
        synchronized (ForeignScreenDetector.class) {
            if (subscribed) {
                return;
            }
            NeoForge.EVENT_BUS.addListener(ForeignScreenDetector::onScreenOpening);
            subscribed = true;
        }
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (LISTENERS.isEmpty()) {
            return;
        }
        Screen screen = event.getNewScreen();
        if (!(screen instanceof MenuAccess<?> menuAccess)) {
            return;
        }
        AbstractContainerMenu menu = menuAccess.getMenu();
        if (menu == null) {
            return;
        }
        ResourceLocation menuTypeId;
        try {
            menuTypeId = BuiltInRegistries.MENU.getKey(menu.getType());
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("Menu of screen {} does not support getType(), skipping foreign screen detection", screen.getClass().getName(), e);
            return;
        }
        if (menuTypeId == null) {
            return;
        }
        List<Consumer<Screen>> callbacks = LISTENERS.get(menuTypeId);
        if (callbacks == null) {
            return;
        }
        for (Consumer<Screen> callback : callbacks) {
            callback.accept(screen);
        }
    }
}
