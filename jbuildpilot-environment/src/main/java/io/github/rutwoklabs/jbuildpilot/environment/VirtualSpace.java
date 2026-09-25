package io.github.rutwoklabs.jbuildpilot.environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents the structured layout of the JBuildPilot root environment.
 */
public class VirtualSpace {
    private final PathManager pathManager;

    public VirtualSpace(PathManager pathManager) {
        this.pathManager = pathManager;
    }

    public void initialize() throws IOException {
        Path home = pathManager.getHomeDirectory();
        Files.createDirectories(home);

        // Core directories for managed artifacts
        Files.createDirectories(pathManager.resolveSafely(home, "jdks"));
        Files.createDirectories(pathManager.resolveSafely(home, "maven"));
        Files.createDirectories(pathManager.resolveSafely(home, "gradle"));
        Files.createDirectories(pathManager.resolveSafely(home, "tools"));
        Files.createDirectories(pathManager.resolveSafely(home, "caches"));
        Files.createDirectories(pathManager.resolveSafely(home, "projects"));
    }

    public PathManager getPathManager() {
        return pathManager;
    }

    public Path getProjectsDirectory() {
        return pathManager.resolveSafely(pathManager.getHomeDirectory(), "projects");
    }
}
