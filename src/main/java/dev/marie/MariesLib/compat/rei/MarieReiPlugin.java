package dev.marie.MariesLib.compat.rei;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.compat.MarieTooltipHelper;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.entry.renderer.EntryRendererRegistry;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

@ApiStatus.Internal
public final class MarieReiPlugin implements REIClientPlugin {

    public static void bootstrap() {
        // Intentionally empty. Called reflectively from client init to keep optional class loading safe.
    }

    @Override
    public void registerEntryRenderers(EntryRendererRegistry registry) {
        if (!ModList.get().isLoaded("roughlyenoughitems")) {
            return;
        }
        registry.transformTooltip(VanillaEntryTypes.ITEM, (stack, point, tooltip) -> {
            ItemStack itemStack = stack.getValue();
            if (itemStack.isEmpty()) {
                return tooltip;
            }
            FoodProperties sourceProperties = itemStack.getItem().components().get(net.minecraft.core.component.DataComponents.FOOD);
            if (sourceProperties == null) {
                return tooltip;
            }
            tooltip.addAllTexts(MarieTooltipHelper.getTooltipLines(itemStack));
            return tooltip;
        });
    }
}
