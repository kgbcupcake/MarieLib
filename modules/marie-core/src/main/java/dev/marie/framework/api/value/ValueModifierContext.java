package dev.marie.framework.api.value;

import dev.marie.framework.api.ApiStatus;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@ApiStatus.Stable
public record ValueModifierContext(
        Player player,
        String playerId,
        ResourceLocation sourceId,
        String valueKey,
        @Nullable Level level,
        @Nullable BlockPos sourcePos
) {
    /**
     * Convenience factory for player-only triggers with no world position.
     *
     * <p>{@link #sourcePos()} will be {@code null} on this path. Positional context
     * is only available when the value pipeline constructs a {@link ValueModifierContext}
     * directly.</p>
     *
     * @since 0.1.0-beta.1
     */
    public static ValueModifierContext of(Player player, ResourceLocation sourceId, String valueKey) {
        return new ValueModifierContext(
                player,
                player.getUUID().toString(),
                sourceId,
                valueKey,
                player.level(),
                null
        );
    }
}
