package io.github.rutwoklabs.jbuildpilot.environment.detector;

import io.github.rutwoklabs.jbuildpilot.core.VersionConstraint;

/**
 * Aggregates specific tool detectors to scan the local machine.
 */
public class EnvironmentDetector {
    private final JavaDetector javaDetector;
    private final MavenDetector mavenDetector;
    private final GradleDetector gradleDetector;

    public EnvironmentDetector(JavaDetector javaDetector, MavenDetector mavenDetector, GradleDetector gradleDetector) {
        this.javaDetector = javaDetector;
        this.mavenDetector = mavenDetector;
        this.gradleDetector = gradleDetector;
    }

    public ToolInfo detectJava(VersionConstraint constraint) {
        return javaDetector.detect(constraint);
    }

    public ToolInfo detectMaven(VersionConstraint constraint) {
        return mavenDetector.detect(constraint);
    }

    public ToolInfo detectGradle(VersionConstraint constraint) {
        return gradleDetector.detect(constraint);
    }
}
