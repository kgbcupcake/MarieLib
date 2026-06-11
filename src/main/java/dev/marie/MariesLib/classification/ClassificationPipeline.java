package dev.marie.MariesLib.classification;

// Called by ItemClassifier for each item — do not call directly from addon code.

/**
 * Identifies which pipeline produced a classification trace.
 */
public enum ClassificationPipeline {
    RUNTIME,
    SCANNER,
    HELD_DEBUG
}
