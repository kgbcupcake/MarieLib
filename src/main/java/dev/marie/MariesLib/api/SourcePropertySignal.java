package dev.marie.MariesLib.api;

import java.util.Map;

import net.minecraft.world.item.ItemStack;

/**
 * A consuming-mod-provided signal that evaluates an arbitrary item
 * property and returns value key contributions.
 *
 * <p>Register via {@link MarieAPI#registerSourcePropertySignal}.</p>
 *
 * <p>Pluggable item property signal for the scanner.
 * The consuming mod decides what "property" means — it could be
 * EMC value, nutrition, block hardness, entity type, anything.</p>
 */
@ApiStatus.Stable
public interface SourcePropertySignal {

    /**
     * A unique identifier for this signal, used in traces and reports.
     * Example: "nourished:nutrition", "emcmod:emc_value"
     */
    String signalId();

    /**
     * Evaluate this signal against an item stack.
     *
     * @param stack the item being classified
     * @return a map of value key → contribution score, or empty map if
     *         this signal does not apply to this item
     */
    Map<String, Float> evaluate(ItemStack stack);
}
