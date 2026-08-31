package dev.marie.framework.ui.api;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.commandcenter.CommandCenterRegistry;
import dev.marie.framework.ui.commandcenter.CommandCenterScreen;
import net.minecraft.client.Minecraft;

/**
 * Public facade for the generic, domain-agnostic command-center screen: a single shared
 * sidebar-navigated dashboard that any number of consumer mods can contribute categories and
 * cards to, rather than each mod building its own standalone settings/quick-actions screen.
 *
 * <p>Contributing content is done ahead of time through {@link CommandCenterRegistry}
 * ({@code registerCategory}/{@code registerCard}/{@code registerCustomCard}, gated to MarieLib's
 * registration phase); this facade's only job is opening the resulting shared screen.
 *
 * <pre>{@code
 * // during mod init, register what this mod contributes:
 * CommandCenterRegistry.registerCategory(new CommandCenterCategory("mymod", Component.literal("My Mod"), 100));
 * CommandCenterRegistry.registerCard(new CommandCenterCard(
 *         "mymod.toggle_feature", "mymod",
 *         Component.literal("Toggle Feature"), Component.literal("Enable/disable the thing"),
 *         0xFF5DA9E9, () -> MyModConfig.toggleFeature()));
 *
 * // wherever the screen should open, e.g. a keybind handler:
 * MarieCommandCenter.openScreen();
 * }</pre>
 */
@ApiStatus.Experimental
public final class MarieCommandCenter {

    private MarieCommandCenter() {}

    /** Opens {@link CommandCenterScreen} over whatever screen is currently open (or none). */
    public static void openScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new CommandCenterScreen(mc.screen));
    }
}
