package io.github.rutwoklabs.jbuildpilot.environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Orchestrates the creation and maintenance of managed ProjectEnvironments.
 */
public class EnvironmentManager {
    private final VirtualSpace virtualSpace;

    public EnvironmentManager(VirtualSpace virtualSpace) {
        this.virtualSpace = virtualSpace;
    }

    /**
     * Initializes the managed space for a project without executing builds.
     */
    public ProjectEnvironment createProjectEnvironment(String projectId, EnvironmentManifest manifest) throws IOException {
        virtualSpace.initialize();

        Path projectsDir = virtualSpace.getProjectsDirectory();
        
        // Ensure path traversal is blocked when setting up a project workspace
        Path projectDir = virtualSpace.getPathManager().resolveSafely(projectsDir, projectId);
        Files.createDirectories(projectDir);

        Path manifestPath = virtualSpace.getPathManager().resolveSafely(projectDir, "manifest.properties");
        manifest.writeTo(manifestPath);

        return new ProjectEnvironment(projectDir, manifest);
    }
}
