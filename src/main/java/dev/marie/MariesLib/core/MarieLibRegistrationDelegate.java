package dev.marie.MariesLib.core;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ThresholdEffect;
import dev.marie.MariesLib.api.ValueDefinition;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Runtime delegate for value/effect registration.
 * Registered by the consuming mod at bootstrap.
 */
@ApiStatus.Stable
public interface MarieLibRegistrationDelegate {

    List<String> getValueKeys();

    void registerValue(ValueDefinition definition);

    void registerEffect(ThresholdEffect definition);

    void registerSourceClassification(ResourceLocation sourceId, String valueKey, float amount);

    /**
     * Returns the {@link ValueDefinition} for the given key, or {@code null} if not registered.
     */
    @Nullable
    default ValueDefinition valueDefinitionFor(String key) {
        return null;
    }
}
