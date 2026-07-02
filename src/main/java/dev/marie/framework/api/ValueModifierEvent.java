package dev.marie.MariesLib.api;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
 *
 * <p>Addon authors subscribe to this event; they do not construct it. The
 * constructor is not part of the stable addon contract.</p>
 */
@ApiStatus.Stable
public class ValueModifierEvent extends Event implements ICancellableEvent {

    private final ValueModifierContext context;
    private float amount;

    /**
     * Constructs a new value modifier event. Intended for MarieLib internal use only.
     *
     * @param context the modifier context describing the player, source, and value
     * @param amount  the original value gain amount before modification
     */
    @ApiStatus.Internal
    public ValueModifierEvent(ValueModifierContext context, float amount) {
        this.context = context;
        this.amount = amount;
    }

    /**
     * Returns the player about to receive the value gain.
     *
     * @return the target player
     */
    public Player getPlayer() {
        return context.player();
    }

    /**
     * Returns the player's UUID as a string.
     *
     * @return the player identifier
     */
    public String getPlayerId() {
        return context.playerId();
    }

    /**
     * Returns the registry identifier of the source providing the value.
     *
     * @return the source's {@link ResourceLocation}
     */
    public ResourceLocation getSourceId() {
        return context.sourceId();
    }

    /**
     * Returns the key of the value being gained.
     *
     * @return the value identifier string
     */
    public String getValueKey() {
        return context.valueKey();
    }

    /**
     * Returns the level associated with this modifier, if any.
     *
     * @return the level, or {@code null} when not applicable
     */
    @Nullable
    public Level getLevel() {
        return context.level();
    }

    /**
     * Returns the world position of the source, if any.
     *
     * @return the source position, or {@code null} when not applicable
     */
    @Nullable
    public BlockPos getSourcePos() {
        return context.sourcePos();
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
