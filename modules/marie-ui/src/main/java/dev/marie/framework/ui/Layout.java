package dev.marie.framework.ui;

import java.util.List;
import java.util.Map;

/**
 * Given an available area and a set of components with their {@link Constraint}s, computes each
 * child's resolved {@link Bounds}. Layouts consume Constraint data; they must not invent their
 * own sizing rules.
 */
public interface Layout {

    Map<MarieComponent, Bounds> computeBounds(Bounds available, List<MarieComponent> children);
}
