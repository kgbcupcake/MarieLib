package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApiStatus.Internal
public final class ScanReportSinkRegistry {

    private static final List<ScanReportSink> SINKS = new CopyOnWriteArrayList<>();

    private ScanReportSinkRegistry() {}

    public static void register(ScanReportSink sink) {
        SINKS.add(sink);
    }

    public static List<ScanReportSink> getSinks() {
        return Collections.unmodifiableList(SINKS);
    }

    public static void clear() {
        SINKS.clear();
    }
}
