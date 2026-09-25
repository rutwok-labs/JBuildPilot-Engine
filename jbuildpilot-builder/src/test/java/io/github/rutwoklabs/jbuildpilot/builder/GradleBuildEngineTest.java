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

class GradleBuildEngineTest {

    @TempDir
    Path projectDir;

    @TempDir
    Path toolHome;

    @TempDir
    Path javaHome;

    private MockProcessRunner runner;
    private PermissionManager permissionManager;
    private GradleBuildEngine engine;

    @BeforeEach
    void setUp() {
        runner = new MockProcessRunner();
        PermissionSet permissions = new PermissionSet();
        permissions.grant(PermissionType.EXECUTE_BUILD);
        permissionManager = new PermissionManager(permissions, null);

        engine = new GradleBuildEngine(permissionManager, runner);
    }

    @Test
    void testCommandConstructionWithWrapper() {
        BuildRequest req = new BuildRequest(projectDir, Arrays.asList("build", "-x", "test"), javaHome, null, true);
        
        BuildResult result = engine.execute(req);
        
        assertTrue(result.isSuccessful());
        List<String> cmd = runner.getLastCommand();
        
        String executable = cmd.get(0);
        assertTrue(executable.contains("gradlew"));
        
        assertEquals("build", cmd.get(1));
        assertEquals("-x", cmd.get(2));
        assertEquals("test", cmd.get(3));
    }

    @Test
    void testCommandConstructionWithGlobalTool() {
        BuildRequest req = new BuildRequest(projectDir, Arrays.asList("assemble"), javaHome, toolHome, false);
        
        BuildResult result = engine.execute(req);
        
        assertTrue(result.isSuccessful());
        List<String> cmd = runner.getLastCommand();
        
        String executable = cmd.get(0);
        assertTrue(executable.contains("bin"));
        assertTrue(executable.contains("gradle"));
        assertEquals("assemble", cmd.get(1));
    }
}
