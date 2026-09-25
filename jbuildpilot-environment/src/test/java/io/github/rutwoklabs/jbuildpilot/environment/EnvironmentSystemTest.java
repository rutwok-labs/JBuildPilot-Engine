package io.github.rutwoklabs.jbuildpilot.environment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentSystemTest {

    @TempDir
    Path tempHome;

    private PathManager pathManager;
    private VirtualSpace virtualSpace;
    private EnvironmentManager environmentManager;

    @BeforeEach
    void setUp() {
        pathManager = new PathManager(tempHome);
        virtualSpace = new VirtualSpace(pathManager);
        environmentManager = new EnvironmentManager(virtualSpace);
    }

    @Test
    void testDirectoryCreation() throws IOException {
        virtualSpace.initialize();

        assertTrue(Files.exists(tempHome.resolve("jdks")), "jdks directory missing");
        assertTrue(Files.exists(tempHome.resolve("maven")), "maven directory missing");
        assertTrue(Files.exists(tempHome.resolve("gradle")), "gradle directory missing");
        assertTrue(Files.exists(tempHome.resolve("caches")), "caches directory missing");
        assertTrue(Files.exists(tempHome.resolve("projects")), "projects directory missing");
    }

    @Test
    void testPathSafety() {
        assertThrows(SecurityException.class, () -> {
            pathManager.resolveSafely(tempHome, "../outside");
        }, "PathManager failed to prevent traversal vulnerability");
    }

    @Test
    void testManifestWritingReading() throws IOException {
        EnvironmentManifest original = new EnvironmentManifest("test-proj", "21", "MAVEN");
        Path manifestFile = tempHome.resolve("manifest.properties");
        
        original.writeTo(manifestFile);
        assertTrue(Files.exists(manifestFile));

        EnvironmentManifest restored = EnvironmentManifest.readFrom(manifestFile);
        assertEquals("test-proj", restored.getProjectId());
        assertEquals("21", restored.getJavaVersion());
        assertEquals("MAVEN", restored.getBuildSystem());
    }

    @Test
    void testProjectEnvironmentCreation() throws IOException {
        EnvironmentManifest manifest = new EnvironmentManifest("demo-project", "17", "GRADLE");
        
        ProjectEnvironment env = environmentManager.createProjectEnvironment("demo-project", manifest);
        
        Path projectDir = tempHome.resolve("projects/demo-project");
        assertEquals(projectDir.toAbsolutePath().normalize(), env.getProjectDirectory().toAbsolutePath().normalize());
        assertTrue(Files.exists(projectDir));
        
        Path manifestPath = projectDir.resolve("manifest.properties");
        assertTrue(Files.exists(manifestPath));
        
        EnvironmentManifest restored = EnvironmentManifest.readFrom(manifestPath);
        assertEquals("GRADLE", restored.getBuildSystem());
        assertEquals("17", restored.getJavaVersion());
    }
}
