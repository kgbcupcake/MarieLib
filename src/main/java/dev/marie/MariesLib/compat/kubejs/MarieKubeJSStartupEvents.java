package dev.marie.MariesLib.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.MarieLibContext;

/**
 * Startup event hook holder for MarieLib KubeJS registration flows.
 */
@ApiStatus.Internal
public final class MarieKubeJSStartupEvents {

    public static final String REGISTER_VALUES = MarieLibContext.get().modId() + ".startup.register_values";
    public static final String REGISTER_PROFILES = MarieLibContext.get().modId() + ".startup.register_profiles";
    public static final String REGISTER_MILESTONES = MarieLibContext.get().modId() + ".startup.register_milestones";

    private static final EventGroup GROUP = EventGroup.of("MarieStartupEvents");

    private MarieKubeJSStartupEvents() {}

    public static void register(EventGroupRegistry registry) {
        GROUP.startup(REGISTER_VALUES, () -> RegisterValuesEvent.class);
        GROUP.startup(REGISTER_PROFILES, () -> RegisterProfilesEvent.class);
        GROUP.startup(REGISTER_MILESTONES, () -> RegisterMilestonesEvent.class);
        registry.register(GROUP);
    }

    public static final class RegisterValuesEvent implements KubeEvent {
        private final MarieKubeJSBindings.ScriptApi api = new MarieKubeJSBindings.ScriptApi();
        public void registerValue(com.google.gson.JsonObject data) { api.registerValue(data); }
        public void registerSourceClassification(String itemId, String valueKey, float amount) { api.registerSourceClassification(itemId, valueKey, amount); }
    }

    public static final class RegisterProfilesEvent implements KubeEvent {
        private final MarieKubeJSBindings.ScriptApi api = new MarieKubeJSBindings.ScriptApi();
        public void registerTrackingProfile(com.google.gson.JsonObject data) { api.registerTrackingProfile(data); }
        public void registerSourcePairSynergy(String sourceA, String sourceB, int windowSeconds, String valueKey, float amount) { api.registerSourcePairSynergy(sourceA, sourceB, windowSeconds, valueKey, amount); }
    }

    public static final class RegisterMilestonesEvent implements KubeEvent {
        private final MarieKubeJSBindings.ScriptApi api = new MarieKubeJSBindings.ScriptApi();
        public void registerMilestone(com.google.gson.JsonObject data) { api.registerMilestone(data); }
    }
}
