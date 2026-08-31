package dev.marie.framework.ui.commandcenter;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.component.MarieComponent;
import net.minecraft.network.chat.Component;

/** Escape hatch for a card needing fully custom content — {@code content} renders inside the card's body region and receives forwarded mouseClicked/mouseScrolled. */
@ApiStatus.Experimental
public record CustomCommandCenterCard(String id, String categoryId, Component title,
                                       MarieComponent content) implements CommandCenterCardEntry {
}
