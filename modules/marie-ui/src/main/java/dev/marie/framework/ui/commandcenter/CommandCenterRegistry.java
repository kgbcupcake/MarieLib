package dev.marie.framework.ui.commandcenter;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marieapi.MarieAPIState;
import dev.marie.framework.registry.AbstractRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Static registry for command-center categories and cards — pluggable, domain-agnostic; a category/card is just an id, a label, and (for a card) a click/content callback. */
@ApiStatus.Experimental
public final class CommandCenterRegistry {

    private static final class Categories extends AbstractRegistry<String, CommandCenterCategory> {
        Categories() {
            super("CommandCenterRegistry.Categories");
        }
    }

    private static final class Cards extends AbstractRegistry<String, CommandCenterCardEntry> {
        Cards() {
            super("CommandCenterRegistry.Cards");
        }
    }

    private static final Categories CATEGORIES = new Categories();
    private static final Cards CARDS = new Cards();

    private CommandCenterRegistry() {}

    /** Freezes both backing registries — mirrors {@code TrackerRegistry}/{@code ColorDefinitionRegistry}'s shape; unlike those, no marie-core bootstrap orchestrator calls this yet (see the accompanying report). */
    @ApiStatus.Internal
    public static void freezeInternal() {
        CATEGORIES.freeze();
        CARDS.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        CATEGORIES.reset();
        CARDS.reset();
    }

    @ApiStatus.Internal
    public static void unfreezeInternal() {
        CATEGORIES.unfreeze();
        CARDS.unfreeze();
    }

    public static boolean isFrozen() {
        return CATEGORIES.isFrozen();
    }

    /** Registers a category; re-registering an id replaces the existing one, same upsert contract as {@code ColorDefinitionRegistry}. */
    public static void registerCategory(CommandCenterCategory category) {
        MarieAPIState.assertRegistrationAllowed("registerCategory");
        CATEGORIES.upsert(category.id(), category);
    }

    /** Registers a standard templated card; re-registering an id replaces the existing one. */
    public static void registerCard(CommandCenterCard card) {
        MarieAPIState.assertRegistrationAllowed("registerCard");
        CARDS.upsert(card.id(), card);
    }

    /** Registers a fully custom card; re-registering an id replaces the existing one. */
    public static void registerCustomCard(CustomCommandCenterCard card) {
        MarieAPIState.assertRegistrationAllowed("registerCustomCard");
        CARDS.upsert(card.id(), card);
    }

    /** Every registered category, sorted by {@link CommandCenterCategory#sortOrder()}. */
    public static List<CommandCenterCategory> categories() {
        List<CommandCenterCategory> sorted = new ArrayList<>(CATEGORIES.values());
        sorted.sort(Comparator.comparingInt(CommandCenterCategory::sortOrder));
        return sorted;
    }

    /** Every card (standard or custom) registered under {@code categoryId}, in registration order. */
    public static List<CommandCenterCardEntry> cardsFor(String categoryId) {
        List<CommandCenterCardEntry> matches = new ArrayList<>();
        for (CommandCenterCardEntry entry : CARDS.values()) {
            if (entry.categoryId().equals(categoryId)) {
                matches.add(entry);
            }
        }
        return matches;
    }
}
