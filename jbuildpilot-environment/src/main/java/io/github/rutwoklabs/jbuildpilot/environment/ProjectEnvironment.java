package io.github.rutwoklabs.jbuildpilot.environment;

import java.nio.file.Path;

/**
 * A specific managed environment isolated for a single project.
 */
public class ProjectEnvironment {
    private final Path projectDirectory;
    private final EnvironmentManifest manifest;

    public ProjectEnvironment(Path projectDirectory, EnvironmentManifest manifest) {
        this.projectDirectory = projectDirectory;
        this.manifest = manifest;
    }

    public Path getProjectDirectory() {
        return projectDirectory;
    }

    public EnvironmentManifest getManifest() {
        return manifest;
    }
}
