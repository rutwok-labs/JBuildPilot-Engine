package io.github.rutwoklabs.jbuildpilot.environment.detector;

import io.github.rutwoklabs.jbuildpilot.core.Version;
import io.github.rutwoklabs.jbuildpilot.core.VersionConstraint;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Detects the currently executing Java environment safely without running external processes.
 */
public class HostJavaDetector implements JavaDetector {
    @Override
    public ToolInfo detect(VersionConstraint constraint) {
        String versionStr = System.getProperty("java.version");
        String homeStr = System.getProperty("java.home");

        if (versionStr == null || homeStr == null) {
            return ToolInfo.missing("java");
        }

        // Simplistic version normalization (e.g. "21.0.1" -> "21" for basic matching if needed)
        // For Phase 5 we rely on the Version object constraint check.
        Version currentVersion = new Version(versionStr);
        Path executable = Paths.get(homeStr, "bin", "java");

        if (constraint.isSatisfiedBy(currentVersion)) {
            return ToolInfo.available("java", versionStr, executable);
        } else {
            return ToolInfo.incompatible("java", versionStr, executable);
        }
    }
}
