package dev.marie.framework.api.marieapi;

import dev.marie.framework.api.registry.ReportProviderRegistry;
import dev.marie.framework.api.reporting.ReportProvider;

final class ReportingRegistrationDelegate {

    private ReportingRegistrationDelegate() {}

    static void registerReportProvider(ReportProvider provider) {
        MarieAPIState.assertRegistrationAllowed("registerReportProvider");
        ReportProviderRegistry.register(provider);
    }
}
