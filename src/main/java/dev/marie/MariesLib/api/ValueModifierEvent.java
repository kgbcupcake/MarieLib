package dev.marie.MariesLib.api;

import dev.marie.MariesLib.api.ApiStatus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired before a value gain is applied to a player, allowing any mod to
 * intercept and multiply, reduce, or cancel the gain dynamically.
 *
 * <p>This event is cancellable. Cancelling it prevents the value gain
 * from being applied entirely. Modifying {@link #setAmount(float)} changes
 * the final value gain value.</p>
 *
 * <p>Subscribe to this event on the NeoForge event bus to implement dynamic
 * value gain modifiers (e.g. potion effects, equipment bonuses, debuffs).</p>
 */
@ApiStatus.Stable
public class ValueModifierEvent extends Event implements ICancellableEvent {

    private final Player player;
    private final ResourceLocation sourceId;
    private final String valueKey;
    private float amount;

    /**
     * Constructs a new value modifier event.
     *
     * @param player      the player about to receive the value gain
     * @param sourceId      the registry identifier of the source providing the value
     * @param valueKey the key of the value being gained
     * @param amount      the original value gain amount before modification
     */
    public ValueModifierEvent(Player player, ResourceLocation sourceId, String valueKey, float amount) {
        this.player = player;
        this.sourceId = sourceId;
        this.valueKey = valueKey;
        this.amount = amount;
    }

    /**
     * Returns the player about to receive the value gain.
     *
     * @return the target player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the registry identifier of the source providing the value.
     *
     * @return the source's {@link ResourceLocation}
     */
    public ResourceLocation getSourceId() {
        return sourceId;
    }

    /**
     * Returns the key of the value being gained.
     *
     * @return the value identifier string
     */
    public String getValueKey() {
        return valueKey;
    }

    /**
     * Returns the current value gain amount (may have been modified by
     * earlier event handlers).
     *
     * @return the current gain amount
     */
    public float getAmount() {
        return amount;
    }

    /**
     * Sets the value gain amount. Use this to multiply, reduce, or zero out
     * the gain. Setting to zero is equivalent to cancelling but allows subsequent
     * handlers to still observe the event.
     *
     * @param amount the new value gain amount (may be zero or negative)
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

}
