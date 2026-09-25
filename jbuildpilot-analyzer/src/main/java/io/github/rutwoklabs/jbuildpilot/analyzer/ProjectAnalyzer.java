package io.github.rutwoklabs.jbuildpilot.analyzer;

import io.github.rutwoklabs.jbuildpilot.core.ProjectModel;
import io.github.rutwoklabs.jbuildpilot.core.ProjectMetadata;

import java.nio.file.Path;
import java.util.Collections;

/**
 * Main entry point for safe, read-only project analysis.
 */
public final class ProjectAnalyzer {

    private final BuildSystemDetector detector;
    private final MavenAnalyzer mavenAnalyzer;
    private final GradleAnalyzer gradleAnalyzer;

    public ProjectAnalyzer() {
        this.detector = new BuildSystemDetector();
        this.mavenAnalyzer = new MavenAnalyzer();
        this.gradleAnalyzer = new GradleAnalyzer();
    }

    /**
     * Analyzes the project directory without executing any external processes or resolving networks.
     * @param projectDir the root directory of the project
     * @return the extracted ProjectModel
     */
    public ProjectModel analyze(Path projectDir) {
        if (projectDir == null) {
            throw new IllegalArgumentException("projectDir cannot be null");
        }

        BuildSystemType systemType = detector.detect(projectDir);

        switch (systemType) {
            case MAVEN:
                return mavenAnalyzer.analyze(projectDir);
            case GRADLE:
                return gradleAnalyzer.analyze(projectDir);
            case UNKNOWN:
            default:
                // No recognized build system. Return an empty project model based on directory name.
                ProjectMetadata metadata = new ProjectMetadata(projectDir.getFileName().toString(), null, null, null);
                return new ProjectModel(metadata, Collections.emptyList());
        }
    }
}
