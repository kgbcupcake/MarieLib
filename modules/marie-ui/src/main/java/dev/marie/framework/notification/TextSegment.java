package dev.marie.framework.notification;

/** One colored run of text, rendered left-to-right with the segments around it on the same line. */
public record TextSegment(String text, int argbColor) {}
