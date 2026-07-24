package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 * Contract for writing scan reports produced by {@link ItemScanner}. Implemented by
 * marie-resources's {@code ScanReportWriter}, which registers itself with
 * {@link ScanReportSinkRegistry} — core has no compile-time dependency on marie-resources,
 * per the locked one-directional module graph.
 */
@ApiStatus.Internal
@FunctionalInterface
public interface ScanReportSink {
    void writeReports(
            List<ClassificationResult> results,
            ScanCache.ScanSummary summary,
            @Nullable ScanCache.ScanDiff diff
    ) throws IOException;
}
