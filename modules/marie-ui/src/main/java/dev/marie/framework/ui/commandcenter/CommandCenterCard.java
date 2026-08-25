package dev.marie.framework.ui.commandcenter;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/** Standard templated card: title/subtitle/accent, optionally clickable. {@code onClick} null means the card is inert/display-only. */
@ApiStatus.Experimental
public record CommandCenterCard(String id, String categoryId, Component title, @Nullable Component subtitle,
                                 int accentColor, @Nullable Runnable onClick) implements CommandCenterCardEntry {
}
