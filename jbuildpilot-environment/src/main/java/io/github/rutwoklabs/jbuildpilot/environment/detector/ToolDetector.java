package io.github.rutwoklabs.jbuildpilot.environment.detector;

import io.github.rutwoklabs.jbuildpilot.core.VersionConstraint;

public interface ToolDetector {
    /**
     * Attempts to detect the tool on the local machine and verify if it satisfies the constraint.
     */
    ToolInfo detect(VersionConstraint constraint);
}
