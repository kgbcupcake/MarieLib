package dev.marie.framework.ui.commandcenter;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.network.chat.Component;

/** One sidebar entry in {@link CommandCenterScreen}; {@code sortOrder} controls its position, lowest first. */
@ApiStatus.Experimental
public record CommandCenterCategory(String id, Component label, int sortOrder) {
}
