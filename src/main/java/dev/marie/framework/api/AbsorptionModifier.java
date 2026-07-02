package dev.marie.MariesLib.api;

import dev.marie.MariesLib.api.ApiStatus;

import net.minecraft.world.entity.player.Player;

/**
 * Modifier interface that adjusts how much of a value a player actually
 * absorbs from a source item, based on their current state.
 *
 * <p>This enables mechanics like reduced absorption under debuffs, boosted
 * absorption under favorable conditions, or diminished gains when impaired.</p>
 *
 * <p>Register implementations via
 * {@link MarieAPI#registerAbsorptionModifier(AbsorptionModifier)}.</p>
 */
@ApiStatus.Experimental
public interface AbsorptionModifier {

    /**
     * Returns a unique identifier for this modifier, used for ordering and debugging.
     *
     * @return a non-null, unique string identifier (e.g. "mymod:modifier_id")
     */
    String getModifierId();

    /**
     * Calculates the absorption multiplier for the given player and value.
     *
     * <p>The returned value multiplies the base value gain. For example:</p>
     * <ul>
     *   <li>{@code 1.0} — no change (default absorption)</li>
     *   <li>{@code 0.5} — player absorbs only half the value</li>
     *   <li>{@code 1.5} — player absorbs 50% more of the value</li>
     *   <li>{@code 0.0} — value is completely blocked</li>
     * </ul>
     *
     * @param player      the player applying the source
     * @param valueKey the internal key of the value being absorbed (e.g. "value_a")
     * @param baseAmount  the unmodified value amount from the source item
     * @return the multiplicative absorption factor; must be non-negative
     */
    float getAbsorptionMultiplier(Player player, String valueKey, float baseAmount);

    /**
     * Returns the priority of this modifier for ordering purposes.
     * Lower values are applied first. Default priority is {@code 0}.
     *
     * @return the priority value for this modifier
     */
    default int getPriority() {
        return 0;
    }
}
