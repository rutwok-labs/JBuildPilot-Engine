package io.github.rutwoklabs.jbuildpilot.environment.detector;

import io.github.rutwoklabs.jbuildpilot.core.RequirementStatus;
import io.github.rutwoklabs.jbuildpilot.core.Version;
import io.github.rutwoklabs.jbuildpilot.core.VersionConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvironmentDetectorTest {

    private EnvironmentDetector detector;

    @BeforeEach
    void setUp() {
        // We use Mock detectors to isolate tests from the physical developer machine
        JavaDetector mockJava = constraint -> {
            if (constraint.isSatisfiedBy(new Version("21"))) {
                return ToolInfo.available("java", "21", Paths.get("/mock/jdk21/bin/java"));
            } else {
                return ToolInfo.incompatible("java", "21", Paths.get("/mock/jdk21/bin/java"));
            }
        };

        MavenDetector mockMaven = constraint -> ToolInfo.missing("maven");
        
        GradleDetector mockGradle = constraint -> {
            if (constraint.isSatisfiedBy(new Version("8.5"))) {
                return ToolInfo.available("gradle", "8.5", Paths.get("/mock/gradle85/bin/gradle"));
            }
            return ToolInfo.incompatible("gradle", "8.5", Paths.get("/mock/gradle85/bin/gradle"));
        };

        detector = new EnvironmentDetector(mockJava, mockMaven, mockGradle);
    }

    @Test
    void testAvailableJava() {
        ToolInfo info = detector.detectJava(VersionConstraint.exact("21"));
        assertEquals(RequirementStatus.AVAILABLE, info.getStatus());
        assertEquals("21", info.getVersion());
    }

    @Test
    void testIncompatibleJava() {
        ToolInfo info = detector.detectJava(VersionConstraint.exact("17"));
        assertEquals(RequirementStatus.INCOMPATIBLE, info.getStatus());
        assertEquals("21", info.getVersion());
    }

    @Test
    void testMissingMaven() {
        ToolInfo info = detector.detectMaven(VersionConstraint.any());
        assertEquals(RequirementStatus.MISSING, info.getStatus());
    }

    @Test
    void testAvailableGradle() {
        ToolInfo info = detector.detectGradle(VersionConstraint.exact("8.5"));
        assertEquals(RequirementStatus.AVAILABLE, info.getStatus());
        assertEquals("8.5", info.getVersion());
    }
}
