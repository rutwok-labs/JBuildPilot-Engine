package io.github.rutwoklabs.jbuildpilot.downloader;

import io.github.rutwoklabs.jbuildpilot.environment.PathManager;
import io.github.rutwoklabs.jbuildpilot.environment.VirtualSpace;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DownloadCache {
    private final VirtualSpace virtualSpace;

    public DownloadCache(VirtualSpace virtualSpace) {
        this.virtualSpace = virtualSpace;
    }

    public Path getCachePath(Artifact artifact) {
        PathManager pm = virtualSpace.getPathManager();
        Path cachesDir = pm.resolveSafely(pm.getHomeDirectory(), "caches");
        return pm.resolveSafely(cachesDir, artifact.getSafeFileName());
    }

    public boolean isCached(Artifact artifact) {
        return Files.exists(getCachePath(artifact));
    }

    /**
     * Atomically moves a verified temporary file into the final cache location.
     */
    public void storeAtomically(Path tempFile, Artifact artifact) throws IOException {
        Path target = getCachePath(artifact);
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        // ATOMIC_MOVE ensures partial writes are never observed by other processes.
        // It is unsupported across filesystems/volumes (temp dir vs. cache dir),
        // so fall back to a replace move when the platform refuses the atomic move.
        try {
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
