package dev.marie.framework.registry;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.IMarieLibConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.core.Holder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@ApiStatus.Internal
public final class MarieAttributes {

    private static DeferredRegister<Attribute> ATTRIBUTES;
    private static DeferredHolder<Attribute, Attribute> VALUE_REGEN_MULTIPLIER;
    private static DeferredHolder<Attribute, Attribute> VALUE_DECAY_MULTIPLIER;
    private static boolean registered = false;

    private MarieAttributes() {}

    private static void ensureInitialized() {
        if (ATTRIBUTES != null) return;
        String modId = IMarieLibConfig.get().modId();
        ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, modId);
        VALUE_REGEN_MULTIPLIER = ATTRIBUTES.register(
                "value_regen_multiplier",
                () -> new RangedAttribute("attribute.name." + modId + ".value_regen_multiplier", 1.0, 0.01, 10.0)
                        .setSyncable(true)
        );
        VALUE_DECAY_MULTIPLIER = ATTRIBUTES.register(
                "value_decay_multiplier",
                () -> new RangedAttribute("attribute.name." + modId + ".value_decay_multiplier", 1.0, 0.01, 10.0)
                        .setSyncable(true)
        );
    }

    public static DeferredRegister<Attribute> attributes() {
        ensureInitialized();
        return ATTRIBUTES;
    }

    public static DeferredHolder<Attribute, Attribute> valueRegenMultiplierHolder() {
        ensureInitialized();
        return VALUE_REGEN_MULTIPLIER;
    }

    public static DeferredHolder<Attribute, Attribute> valueDecayMultiplierHolder() {
        ensureInitialized();
        return VALUE_DECAY_MULTIPLIER;
    }

    public static void register(IEventBus modEventBus) {
        if (registered) return;
        registered = true;
        ensureInitialized();
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(MarieAttributes::onEntityAttributeModification);
    }

    private static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, valueRegenMultiplierHolder());
        event.add(EntityType.PLAYER, valueDecayMultiplierHolder());
    }

    public static float valueRegenMultiplier(ServerPlayer player) {
        return attributeMultiplier(player, valueRegenMultiplierHolder());
    }

    public static float valueDecayMultiplier(ServerPlayer player) {
        return attributeMultiplier(player, valueDecayMultiplierHolder());
    }

    private static float attributeMultiplier(ServerPlayer player, Holder<Attribute> attribute) {
        var inst = player.getAttribute(attribute);
        return inst == null ? 1.0f : (float) inst.getValue();
    }
}
