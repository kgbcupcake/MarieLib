package dev.marie.MariesLib.api;

import dev.marie.MariesLib.api.ApiStatus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Contains all NeoForge event classes fired by the MarieLib value system.
 *
 * <p>Subscribe to these events via {@code @SubscribeEvent} on the NeoForge event bus
 * to react to value changes, critical states, and source consumption.</p>
 */
public final class MarieEvents {

    private MarieEvents() {}

    /**
     * Fired when a player's value level changes value.
     *
     * <p>This event is fired after the value value has been updated. It is
     * not cancellable. Use {@link ValueModifierEvent} to intercept changes
     * before they are applied.</p>
     */
    @ApiStatus.Stable
    public static class ValueChangedEvent extends Event {

        private final Player player;
        private final String valueKey;
        private final float oldValue;
        private final float newValue;

        /**
         * Constructs a new value changed event.
         *
         * @param player      the player whose value level changed
         * @param valueKey the key of the value that changed
         * @param oldValue    the previous value level
         * @param newValue    the new value level
         */
        public ValueChangedEvent(Player player, String valueKey, float oldValue, float newValue) {
            this.player = player;
            this.valueKey = valueKey;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        /**
         * Returns the player whose value level changed.
         *
         * @return the affected player
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * Returns the key of the value that changed.
         *
         * @return the value identifier string
         */
        public String getValueKey() {
            return valueKey;
        }

        /**
         * Returns the previous value level before the change.
         *
         * @return the old value as a normalized float
         */
        public float getOldValue() {
            return oldValue;
        }

        /**
         * Returns the new value level after the change.
         *
         * @return the new value as a normalized float
         */
        public float getNewValue() {
            return newValue;
        }
    }

    /**
     * Fired when a player's value level drops below the critical threshold.
     *
     * <p>This is an informational event fired once per threshold crossing
     * (not every tick while critical). Not cancellable.</p>
     */
    @ApiStatus.Stable
    public static class ValueCriticalEvent extends Event {

        private final Player player;
        private final String valueKey;

        /**
         * Constructs a new value critical event.
         *
         * @param player      the player whose value became critical
         * @param valueKey the key of the value that crossed the critical threshold
         */
        public ValueCriticalEvent(Player player, String valueKey) {
            this.player = player;
            this.valueKey = valueKey;
        }

        /**
         * Returns the player whose value reached critical level.
         *
         * @return the affected player
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * Returns the key of the value that is critically low.
         *
         * @return the value identifier string
         */
        public String getValueKey() {
            return valueKey;
        }
    }

    /**
     * Fired when a player's value level exceeds the excess threshold.
     *
     * <p>This is an informational event fired once per threshold crossing
     * (not every tick while excessive). Not cancellable.</p>
     */
    @ApiStatus.Stable
    public static class ValueExcessEvent extends Event {

        private final Player player;
        private final String valueKey;

        /**
         * Constructs a new value excess event.
         *
         * @param player      the player whose value became excessive
         * @param valueKey the key of the value that crossed the excess threshold
         */
        public ValueExcessEvent(Player player, String valueKey) {
            this.player = player;
            this.valueKey = valueKey;
        }

        /**
         * Returns the player whose value reached excess level.
         *
         * @return the affected player
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * Returns the key of the value that is excessively high.
         *
         * @return the value identifier string
         */
        public String getValueKey() {
            return valueKey;
        }
    }

    /**
     * Fired before the value pipeline processes a trigger.
     * Cancellable — cancel to prevent all value changes for this trigger.
     */
    @ApiStatus.Stable
    public static class SourceTriggerEvent extends Event implements ICancellableEvent {

        private final Player player;
        private final ValueSourceTrigger trigger;

        public SourceTriggerEvent(Player player, ValueSourceTrigger trigger) {
            this.player = player;
            this.trigger = trigger;
        }

        public Player getPlayer() { return player; }
        public ValueSourceTrigger getTrigger() { return trigger; }
    }

    /**
     * Fired when a player applies a source item and gains value from it.
     *
     * <p>This event is fired after value gains are calculated but the
     * informational snapshot is taken at the point of application. Not cancellable.</p>
     */
    @ApiStatus.Stable
    public static class SourceAppliedEvent extends Event {

        private final Player player;
        private final ResourceLocation sourceId;
        private final String valueKey;
        private final float amount;

        /**
         * Constructs a new source applied event.
         *
         * @param player      the player who applied the source
         * @param sourceId      the registry identifier of the consumed source item
         * @param valueKey the primary value gained from this source
         * @param amount      the amount of value gained
         */
        public SourceAppliedEvent(Player player, ResourceLocation sourceId, String valueKey, float amount) {
            this.player = player;
            this.sourceId = sourceId;
            this.valueKey = valueKey;
            this.amount = amount;
        }

        /**
         * Returns the player who applied the source.
         *
         * @return the player
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * Returns the registry identifier of the consumed source item.
         *
         * @return the source's {@link ResourceLocation}
         */
        public ResourceLocation getSourceId() {
            return sourceId;
        }

        /**
         * Returns the primary value key gained from this source.
         *
         * @return the value identifier string
         */
        public String getValueKey() {
            return valueKey;
        }

        /**
         * Returns the amount of value gained from this source consumption.
         *
         * @return the value gain amount
         */
        public float getAmount() {
            return amount;
        }
    }
}
