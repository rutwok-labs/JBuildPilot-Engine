package io.github.rutwoklabs.jbuildpilot.builder;

import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionDecision;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionSet;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MavenBuildEngineTest {

    @TempDir
    Path projectDir;

    @TempDir
    Path toolHome;

    @TempDir
    Path javaHome;

    private MockProcessRunner runner;
    private PermissionManager permissionManager;
    private MavenBuildEngine engine;

    @BeforeEach
    void setUp() {
        runner = new MockProcessRunner();
        // Give explicit EXECUTE_BUILD permission
        PermissionSet permissions = new PermissionSet();
        permissions.grant(PermissionType.EXECUTE_BUILD);
        permissionManager = new PermissionManager(permissions, null);

        engine = new MavenBuildEngine(permissionManager, runner);
    }

    @Test
    void testExecutionDeniedWithoutPermission() {
        PermissionManager strictManager = new PermissionManager(new PermissionSet(), null);
        MavenBuildEngine strictEngine = new MavenBuildEngine(strictManager, runner);
        
        BuildRequest req = new BuildRequest(projectDir, List.of("clean", "install"), javaHome, null, true);
        assertThrows(SecurityException.class, () -> strictEngine.execute(req));
    }

    @Test
    void testCommandConstructionWithWrapper() {
        BuildRequest req = new BuildRequest(projectDir, Arrays.asList("clean", "package", "-DskipTests"), javaHome, null, true);
        
        BuildResult result = engine.execute(req);
        
        assertTrue(result.isSuccessful());
        List<String> cmd = runner.getLastCommand();
        
        // Assert executable base name
        String executable = cmd.get(0);
        assertTrue(executable.contains("mvnw"));
        
        // Assert arguments
        assertEquals("clean", cmd.get(1));
        assertEquals("package", cmd.get(2));
        assertEquals("-DskipTests", cmd.get(3));
        
        // Assert environments
        assertEquals(projectDir, runner.getLastDirectory());
        assertEquals(javaHome, runner.getLastJavaHome());
    }

    @Test
    void testCommandConstructionWithGlobalTool() {
        BuildRequest req = new BuildRequest(projectDir, Arrays.asList("test"), javaHome, toolHome, false);
        
        BuildResult result = engine.execute(req);
        
        assertTrue(result.isSuccessful());
        List<String> cmd = runner.getLastCommand();
        
        // Assert executable starts with toolHome/bin
        String executable = cmd.get(0);
        assertTrue(executable.contains("bin"));
        assertTrue(executable.contains("mvn"));
        assertEquals("test", cmd.get(1));
    }
}
