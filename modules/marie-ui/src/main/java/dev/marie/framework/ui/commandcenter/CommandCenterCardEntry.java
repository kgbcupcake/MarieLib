package dev.marie.framework.ui.commandcenter;

import dev.marie.framework.api.ApiStatus;

/** Common identity shared by {@link CommandCenterCard} and {@link CustomCommandCenterCard}, so the registry can store/order both together. */
@ApiStatus.Experimental
public sealed interface CommandCenterCardEntry permits CommandCenterCard, CustomCommandCenterCard {

    String id();

    String categoryId();
}
