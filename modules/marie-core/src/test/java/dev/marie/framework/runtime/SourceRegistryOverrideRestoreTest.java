package dev.marie.framework.runtime;

import dev.marie.framework.api.marieapi.MarieAPIState;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression coverage for the data-loss bug where applying and then removing an authoritative
 * classification override destroyed a genuine {@link SourceRegistry#registerClassification}
 * entry that existed for the same sourceId beforehand.
 */
class SourceRegistryOverrideRestoreTest {

    private final ResourceLocation sourceId = ResourceLocation.fromNamespaceAndPath("marieslib_test", "grain_thing");

    @BeforeEach
    void resetState() {
        // Default phase is MOD_INIT (not DATAPACK_RELOAD), so registerClassification() mirrors into
        // the API-registered map — exactly the "genuine registration" case under test.
        SourceRegistry.unregisterClassification(sourceId);
        SourceRegistry.clearScannerClassifications();
    }

    @AfterEach
    void tearDown() {
        SourceRegistry.unregisterClassification(sourceId);
    }

    @Test
    void removingOverrideRestoresGenuineRegistrationUnderneathIt() {
        assertEquals(MarieAPIState.Phase.MOD_INIT, MarieAPIState.getPhase(),
                "precondition: registration phase must not be DATAPACK_RELOAD");

        SourceRegistry.registerClassification(sourceId, "grains", 1.0f);
        assertEquals(Map.of("grains", 1.0f), SourceRegistry.getExternalClassification(sourceId));

        SourceRegistry.applyAuthoritativeOverride(sourceId, Map.of("proteins", 1.0f));
        assertEquals(Map.of("proteins", 1.0f), SourceRegistry.getExternalClassification(sourceId),
                "override should shadow the genuine registration while active");

        SourceRegistry.unregisterClassification(sourceId);

        assertEquals(Map.of("grains", 1.0f), SourceRegistry.getExternalClassification(sourceId),
                "removing the override must restore the genuine registration, not delete the source");
    }

    @Test
    void removingOverrideWithNoPriorRegistrationStillClearsTheSource() {
        SourceRegistry.applyAuthoritativeOverride(sourceId, Map.of("proteins", 1.0f));
        assertEquals(Map.of("proteins", 1.0f), SourceRegistry.getExternalClassification(sourceId));

        SourceRegistry.unregisterClassification(sourceId);

        assertNull(SourceRegistry.getExternalClassification(sourceId),
                "with no genuine registration underneath, unregister should drop the source entirely");
    }

    @Test
    void genuineRegistrationSurvivesRepeatedOverridePushes() {
        SourceRegistry.registerClassification(sourceId, "grains", 1.0f);

        // Simulates SourceClassificationRegistry.pushToSourceRegistry() running once per datapack
        // reload: unregister the stale bridged entry, then re-apply the override.
        for (int i = 0; i < 3; i++) {
            SourceRegistry.applyAuthoritativeOverride(sourceId, Map.of("proteins", 1.0f));
            SourceRegistry.unregisterClassification(sourceId);
        }

        assertEquals(Map.of("grains", 1.0f), SourceRegistry.getExternalClassification(sourceId),
                "genuine registration must survive any number of override apply/remove cycles");
    }
}
