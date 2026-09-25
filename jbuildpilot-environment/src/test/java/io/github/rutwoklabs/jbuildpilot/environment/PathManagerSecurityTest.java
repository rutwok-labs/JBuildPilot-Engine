package io.github.rutwoklabs.jbuildpilot.environment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathManagerSecurityTest {

    @Test
    void testBasicDirectoryTraversalThwarted(@TempDir Path tempDir) {
        PathManager manager = new PathManager(tempDir);
        
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            manager.resolveSafely(tempDir, "../../etc/passwd");
        });
        assertTrue(ex.getMessage().contains("Path traversal detected"));
    }

    @Test
    void testAbsoluteChildPathThwarted(@TempDir Path tempDir) {
        PathManager manager = new PathManager(tempDir);
        
        // Passing an absolute path to resolve() might trick it into returning that absolute path
        String absolutePayload = Paths.get("/etc/shadow").toAbsolutePath().toString();
        
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            manager.resolveSafely(tempDir, absolutePayload);
        });
        assertTrue(ex.getMessage().contains("Path traversal detected"));
    }
}
