package io.github.rutwoklabs.jbuildpilot.environment;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages filesystem paths and provides security against path traversal attacks.
 */
public class PathManager {
    private final Path homeDirectory;

    public PathManager() {
        this(Paths.get(System.getProperty("user.home"), ".jbuildpilot"));
    }

    public PathManager(Path overrideHome) {
        this.homeDirectory = overrideHome.toAbsolutePath().normalize();
    }

    public Path getHomeDirectory() {
        return homeDirectory;
    }

    /**
     * Resolves a child path against a base path safely.
     * Prevents path traversal vulnerabilities (e.g., "../").
     */
    public Path resolveSafely(Path base, String child) {
        Path resolved = base.resolve(child).toAbsolutePath().normalize();
        
        if (!resolved.startsWith(base.toAbsolutePath().normalize())) {
            throw new SecurityException("Path traversal detected! Attempted to resolve: " + child + " outside of " + base);
        }
        
        return resolved;
    }
}
