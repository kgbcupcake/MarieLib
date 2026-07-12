package dev.marie.framework.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Extension point for producing "what's at this block" tooltip-style data.
 *
 * <p>Registered via {@link MarieAPI#registerBlockHoverProvider(BlockHoverProvider)}. The client
 * requests data for a block via a request/response network round-trip (never a direct
 * client-side capability read); the server resolves the first matching provider and returns
 * its data for client-side rendering.</p>
 */
@ApiStatus.Experimental
public interface BlockHoverProvider {

    /**
     * Whether this provider can produce data for the given block. Must be safe to call with
     * only client-visible information — block and blockstate identity only, never a capability
     * value, since this may be invoked client-side to short-circuit a request.
     */
    @ApiStatus.Experimental
    boolean supports(Level level, BlockPos pos, BlockState state);

    /**
     * Computes the server-side data for the given block. The only method in this contract
     * allowed to touch a capability. Called only after {@link #supports} matched.
     */
    @ApiStatus.Experimental
    CompoundTag computeServerData(ServerLevel level, BlockPos pos, ServerPlayer player);

    /**
     * Formats previously computed server data into display lines. Pure formatting — must not
     * touch a capability or any server-only state.
     */
    @ApiStatus.Experimental
    List<Component> renderLines(CompoundTag data, Level level, BlockPos pos);
}
