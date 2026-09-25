package io.github.rutwoklabs.jbuildpilot.environment;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ToolInstaller {
    private final PathManager pathManager;

    public ToolInstaller(PathManager pathManager) {
        this.pathManager = pathManager;
    }

    public Path installTool(String toolId, Path archiveFile) throws Exception {
        Path toolsDir = pathManager.resolveSafely(pathManager.getHomeDirectory(), "tools");
        Path targetDir = pathManager.resolveSafely(toolsDir, toolId);

        if (Files.exists(targetDir)) {
            // Cleanup existing or failed install
            deleteDirectory(targetDir);
        }
        Files.createDirectories(targetDir);

        try (InputStream is = Files.newInputStream(archiveFile);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolvedPath = targetDir.resolve(entry.getName()).normalize();
                if (!resolvedPath.startsWith(targetDir)) {
                    throw new SecurityException("Path traversal attempt in archive: " + entry.getName());
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    if (resolvedPath.getParent() != null) {
                        Files.createDirectories(resolvedPath.getParent());
                    }
                    Files.copy(zis, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        // Validate executable (e.g., bin/mvn or bin/gradle).
        // Note: the Maven distribution ships its launcher as "mvn", not "maven",
        // so derive the expected launcher name from the tool base name.
        String base = toolId.split("-")[0].toLowerCase();
        String launcher = "maven".equals(base) ? "mvn" : base;
        boolean hasExecutable;
        try (var walk = Files.walk(targetDir)) {
            hasExecutable = walk
                .map(p -> p.getFileName().toString().toLowerCase())
                .anyMatch(n -> n.startsWith(base) || n.startsWith(launcher));
        }

        if (!hasExecutable) {
            deleteDirectory(targetDir);
            throw new IllegalStateException("Installed tool has no valid executable in expected paths.");
        }

        return targetDir;
    }

    private void deleteDirectory(Path path) throws Exception {
        if (!Files.exists(path)) return;
        try (var walk = Files.walk(path)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(java.io.File::delete);
        }
    }
}
