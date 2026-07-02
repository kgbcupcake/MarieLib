package dev.marie.MariesLib.scanner.analysis;

import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

/**
 * A source item that qualifies for a secondary value tag recommendation.
 *
 * @param itemId  The item's registry ID
 * @param score   The secondary value's weighted score
 * @param dominant The item's dominant value category
 */
@ApiStatus.Internal
public record MultiValueEntry(
        ResourceLocation itemId,
        float score,
        String dominant
) {}
