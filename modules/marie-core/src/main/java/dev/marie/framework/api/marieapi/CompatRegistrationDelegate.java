package dev.marie.framework.api.marieapi;

import dev.marie.framework.compat.CompatDefinition;

final class CompatRegistrationDelegate {

    private CompatRegistrationDelegate() {}

    static void registerCompatEntry(CompatDefinition definition) {
        MarieAPIState.assertRegistrationAllowed("registerCompatEntry");
        dev.marie.framework.compat.ModCompat.registerExternal(definition);
    }
}
