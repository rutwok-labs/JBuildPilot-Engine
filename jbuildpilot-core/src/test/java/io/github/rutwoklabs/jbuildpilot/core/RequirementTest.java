package io.github.rutwoklabs.jbuildpilot.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequirementTest {

    @Test
    void shouldCreateJavaRequirement() {
        Requirement javaReq = new JavaRequirement("21");
        assertEquals(RequirementType.JAVA, javaReq.getType());
        assertEquals("java", javaReq.getIdentity());
        assertEquals(RequirementStatus.UNKNOWN, javaReq.getStatus());
        assertTrue(javaReq.getVersionConstraint().isSatisfiedBy(Version.of("21")));
    }

    @Test
    void shouldUpdateRequirementStatusImmutably() {
        Requirement mavenReq = new MavenRequirement("3.9.5");
        assertEquals(RequirementStatus.UNKNOWN, mavenReq.getStatus());

        Requirement updatedReq = mavenReq.withStatus(RequirementStatus.AVAILABLE);
        
        assertEquals(RequirementStatus.AVAILABLE, updatedReq.getStatus());
        assertEquals(RequirementStatus.UNKNOWN, mavenReq.getStatus()); // Original remains unchanged
        assertNotSame(mavenReq, updatedReq);
    }

    @Test
    void shouldCreateDependencyRequirement() {
        DependencyRequirement depReq = new DependencyRequirement("org.lwjgl", "lwjgl", "3.3.3");
        assertEquals("org.lwjgl:lwjgl", depReq.getIdentity());
        assertEquals(RequirementType.DEPENDENCY, depReq.getType());
        assertEquals("org.lwjgl", depReq.getGroupId());
        assertEquals("lwjgl", depReq.getArtifactId());
    }

    @Test
    void shouldCreatePluginRequirement() {
        PluginRequirement pluginReq = new PluginRequirement("org.springframework.boot", "3.1.2");
        assertEquals("org.springframework.boot", pluginReq.getIdentity());
        assertEquals(RequirementType.PLUGIN, pluginReq.getType());
        assertEquals(RequirementStatus.UNKNOWN, pluginReq.getStatus());
    }
}
