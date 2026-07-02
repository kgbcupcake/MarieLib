package dev.marie.framework.data;

import dev.marie.framework.api.MarieAPI;
import dev.marie.framework.api.MilestoneDefinition;
import dev.marie.framework.api.ProfileDefinition;
import dev.marie.framework.api.SourcePairSynergy;
import dev.marie.framework.api.SynergyDefinition;
import dev.marie.framework.api.ThresholdEffect;
import dev.marie.framework.api.ValueDefinition;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.compat.CompatDefinition;
import dev.marie.framework.config.LockRegistry;
import dev.marie.framework.runtime.FamilyResolver;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default {@link MarieDataLoader.Callbacks} implementation that delegates datapack
 * registrations to {@link MarieAPI}.
 */
@ApiStatus.Internal
public final class MarieDatapackCallbacks implements MarieDataLoader.Callbacks {

    public static final MarieDatapackCallbacks INSTANCE = new MarieDatapackCallbacks();

    private MarieDatapackCallbacks() {}

    @Override
    public void registerValue(ValueDefinition def) {
        MarieAPI.registerValue(def);
    }

    @Override
    public void registerSourceClassification(ResourceLocation itemId, String valueKey, float amount) {
        MarieAPI.registerSourceClassification(itemId, valueKey, amount);
    }

    @Override
    public void registerCustomEffect(ThresholdEffect def) {
        MarieAPI.registerCustomEffect(def);
    }

    @Override
    public void registerValueSynergy(SynergyDefinition def) {
        MarieAPI.registerValueSynergy(def);
    }

    @Override
    public void registerSourcePairSynergy(SourcePairSynergy def) {
        MarieAPI.registerSourcePairSynergy(def);
    }

    @Override
    public void registerMilestone(MilestoneDefinition def) {
        MarieAPI.registerMilestone(def);
    }

    @Override
    public void registerTrackingProfile(ProfileDefinition def) {
        MarieAPI.registerTrackingProfile(def);
    }

    @Override
    public void registerCompatEntry(CompatDefinition def) {
        MarieAPI.registerCompatEntry(def);
    }

    @Override
    public void replaceSourceFamilies(Map<String, List<String>> families) {
        FamilyResolver.replaceFamilies(families);
    }

    @Override
    public void replaceModuleLocks(Set<String> locked, Set<String> serverOnly) {
        LockRegistry.replaceFromDatapack(locked, serverOnly);
    }
}
