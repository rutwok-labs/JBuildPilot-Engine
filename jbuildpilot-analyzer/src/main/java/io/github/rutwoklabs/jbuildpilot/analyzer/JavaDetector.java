package io.github.rutwoklabs.jbuildpilot.analyzer;

import io.github.rutwoklabs.jbuildpilot.core.JavaRequirement;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Common interface for extracting Java requirements safely.
 */
public interface JavaDetector {
    
    /**
     * Attempts to find a Java requirement in the project directory.
     * Must be read-only and static (no code execution).
     */
    Optional<JavaRequirement> detectJavaVersion(Path projectDir);
}
