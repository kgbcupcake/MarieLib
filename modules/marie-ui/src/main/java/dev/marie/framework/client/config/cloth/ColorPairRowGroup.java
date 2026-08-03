package dev.marie.framework.client.config.cloth;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.color.ColorKeyPair;
import net.minecraft.network.chat.Component;

/**
 * Builds the pair of {@link ColorHexRowWidget} rows (background, text) for a
 * {@link ColorKeyPair}, so callers wiring up a Cloth Config category don't have to construct
 * both rows by hand with matching labels/positioning.
 */
@ApiStatus.Stable
public final class ColorPairRowGroup {

    private ColorPairRowGroup() {}

    /**
     * Builds the background and text rows for {@code pair}, ready to add to a Cloth Config category.
     *
     * @param pair            the background/text color key pair
     * @param backgroundLabel label for the background row
     * @param textLabel       label for the text row
     * @return the two rows, background first then text
     */
    @ApiStatus.Stable
    public static ColorHexRowWidget[] buildRows(ColorKeyPair pair, Component backgroundLabel, Component textLabel) {
        return new ColorHexRowWidget[] {
                new ColorHexRowWidget(pair.background(), backgroundLabel),
                new ColorHexRowWidget(pair.text(), textLabel)
        };
    }
}
