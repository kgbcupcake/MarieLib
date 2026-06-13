package dev.marie.MariesLib.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.compat.MarieTooltipHelper;
import dev.marie.MariesLib.core.MarieLibContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

@ApiStatus.Internal
public final class MarieEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        if (!ModList.get().isLoaded("emi")) {
            return;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!MarieLibContext.isRegistered() || !MarieLibContext.get().sourceItemFilter().test(stack)) {
                continue;
            }
            int tooltipHash = MarieTooltipHelper.getTooltipLines(stack).hashCode();
            if (tooltipHash != 0) {
                registry.setDefaultComparison(item, previous -> Comparison.compareComponents());
            }
        }
    }
}
