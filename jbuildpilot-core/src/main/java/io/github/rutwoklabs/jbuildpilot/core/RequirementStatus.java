package io.github.rutwoklabs.jbuildpilot.core;

/**
 * Represents the resolution status of a requirement.
 */
public enum RequirementStatus {
    AVAILABLE,
    MISSING,
    INCOMPATIBLE,
    UNKNOWN,
    OPTIONAL,
    REQUIRED
}
