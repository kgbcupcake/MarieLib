package dev.marie.framework.client;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.BlockHoverProvider;
import dev.marie.framework.network.BlockHoverResponsePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Single-slot client cache for the block currently under the crosshair. Not a general
 * BlockPos-keyed map — only the currently tracked block is ever relevant to a future renderer.
 */
@ApiStatus.Internal
public final class BlockHoverClientCache {

    private static volatile BlockPos trackedPos;
    private static volatile BlockHoverProvider trackedProvider;
    private static volatile Result current = Result.EMPTY;

    private BlockHoverClientCache() {}

    public record Result(boolean found, CompoundTag data, long receivedAtMs) {
        static final Result EMPTY = new Result(false, new CompoundTag(), 0L);
    }

    /**
     * Switches the tracked block and the provider that matched it, discarding any previously
     * cached result for the old block.
     */
    public static void setTrackedPos(BlockPos pos, BlockHoverProvider provider) {
        trackedPos = pos;
        trackedProvider = provider;
        current = Result.EMPTY;
    }

    public static void onResponse(BlockHoverResponsePayload payload) {
        // A response can arrive after the player has already looked away from its pos; it
        // belongs to a request that's no longer relevant, so it must not overwrite the cache.
        if (!payload.pos().equals(trackedPos)) {
            return;
        }
        current = new Result(payload.found(), payload.data(), System.currentTimeMillis());
    }

    public static Result getCurrent() {
        return current;
    }

    public static BlockPos getTrackedPos() {
        return trackedPos;
    }

    /**
     * Formatted lines for the currently tracked block, or empty if no response has arrived yet,
     * the response was a no-match, or nothing is tracked. Reuses the provider {@link BlockHoverTracker}
     * already resolved via {@code supports()} instead of re-deriving it here.
     */
    public static List<Component> getRenderLines(Level level) {
        BlockPos pos = trackedPos;
        BlockHoverProvider provider = trackedProvider;
        Result result = current;
        if (pos == null || provider == null || !result.found()) {
            return List.of();
        }
        return provider.renderLines(result.data(), level, pos);
    }

    /** Called when the player isn't looking at any block, or no provider matched it. */
    public static void clear() {
        trackedPos = null;
        trackedProvider = null;
        current = Result.EMPTY;
    }
}
