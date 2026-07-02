package dev.marie.framework.scanner;

import dev.marie.framework.api.ApiStatus;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApiStatus.Internal
public final class TagRecommendationSinkRegistry {

    private static final List<TagRecommendationSink> SINKS = new CopyOnWriteArrayList<>();

    private TagRecommendationSinkRegistry() {}

    public static void register(TagRecommendationSink sink) {
        SINKS.add(sink);
    }

    public static List<TagRecommendationSink> getSinks() {
        return Collections.unmodifiableList(SINKS);
    }

    public static void clear() {
        SINKS.clear();
    }
}
