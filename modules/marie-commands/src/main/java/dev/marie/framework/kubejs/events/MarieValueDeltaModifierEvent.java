package dev.marie.framework.kubejs.events;


import javax.annotation.Nullable;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.value.ValueModifierContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@ApiStatus.Experimental
public class MarieValueDeltaModifierEvent implements KubeEvent {

    public final ValueModifierContext context;
    private float amount;

    public MarieValueDeltaModifierEvent() {
        this.context = new ValueModifierContext(
                null,
                "",
                ResourceLocation.withDefaultNamespace("empty"),
                "",
                null,
                null
        );
        this.amount = 0f;
    }

    public MarieValueDeltaModifierEvent(ValueModifierContext context, float amount) {
        this.context = context;
        this.amount = amount;
    }

    public Player getPlayer() {
        return context.player();
    }

    public String getPlayerId() {
        return context.playerId();
    }

    public ResourceLocation getSourceId() {
        return context.sourceId();
    }

    /**
     * Returns the string form of {@link #getSourceId()} for script compatibility.
     * The source is not necessarily an item — it is the registry id of whatever
     * triggered the value change.
     */
    public String getItemId() {
        return context.sourceId().toString();
    }

    public String getValueKey() {
        return context.valueKey();
    }

    @Nullable
    public Level getLevel() {
        return context.level();
    }

    @Nullable
    public BlockPos getSourcePos() {
        return context.sourcePos();
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void cancel() {
        this.amount = 0f;
    }
}
