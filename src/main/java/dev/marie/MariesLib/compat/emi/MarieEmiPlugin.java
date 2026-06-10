package dev.marie.MariesLib.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.compat.MarieTooltipHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

@ApiStatus.Internal
public final class MarieEmiPlugin implements EmiPlugin {

    public static void bootstrap() {
        // Intentionally empty. Called reflectively from client init to keep optional class loading safe.
    }

    @Override
    public void register(EmiRegistry registry) {
        if (!ModList.get().isLoaded("emi")) {
            return;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            FoodProperties sourceProperties = item.components().get(net.minecraft.core.component.DataComponents.FOOD);
            if (sourceProperties == null) {
                continue;
            }
            int tooltipHash = MarieTooltipHelper.getTooltipLines(stack).hashCode();
            if (tooltipHash != 0) {
                registry.setDefaultComparison(item, previous -> Comparison.compareComponents());
            }
        }
    }
}
