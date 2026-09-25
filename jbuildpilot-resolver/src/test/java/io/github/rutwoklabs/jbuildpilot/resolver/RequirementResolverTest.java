package io.github.rutwoklabs.jbuildpilot.resolver;

import io.github.rutwoklabs.jbuildpilot.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RequirementResolverTest {

    private RequirementResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RequirementResolver();
    }

    @Test
    void testSimpleMavenProject() {
        ProjectModel model = new ProjectModel(
                new ProjectMetadata("maven-demo", null, null, null),
                Arrays.asList(
                        new MavenRequirement(VersionConstraint.any(), RequirementStatus.UNKNOWN),
                        new JavaRequirement("17")
                )
        );

        ResolutionResult result = resolver.resolve(model);
        assertTrue(result.isSuccessful());
        
        RequirementGraph graph = result.getGraph();
        assertEquals(3, graph.getNodes().size()); // Root + Maven + Java
        
        assertTrue(graph.getEdges().stream().anyMatch(e -> e.getRelationshipType().equals("BUILT_WITH")));
        assertTrue(graph.getEdges().stream().anyMatch(e -> e.getRelationshipType().equals("RUNS_ON")));
    }

    @Test
    void testSimpleGradleProject() {
        ProjectModel model = new ProjectModel(
                new ProjectMetadata("gradle-demo", null, null, null),
                Arrays.asList(
                        new GradleRequirement(VersionConstraint.any(), RequirementStatus.UNKNOWN),
                        new JavaRequirement("21")
                )
        );

        ResolutionResult result = resolver.resolve(model);
        assertTrue(result.isSuccessful());
        assertEquals(3, result.getGraph().getNodes().size());
    }

    @Test
    void testMissingJavaRequirement() {
        ProjectModel model = new ProjectModel(
                new ProjectMetadata("missing-java", null, null, null),
                Collections.singletonList(
                        new GradleRequirement(VersionConstraint.any(), RequirementStatus.UNKNOWN)
                )
        );

        ResolutionResult result = resolver.resolve(model);
        assertFalse(result.isSuccessful()); // No Java means resolution failure

        boolean hasMissingJavaNode = result.getGraph().getNodes().stream()
                .anyMatch(n -> n.getStatus() == RequirementStatus.MISSING && n.getId().equals("MISSING-JAVA"));
        assertTrue(hasMissingJavaNode);
    }

    @Test
    void testDependencyGraph() {
        ProjectModel model = new ProjectModel(
                new ProjectMetadata("multi-deps", null, null, null),
                Arrays.asList(
                        new JavaRequirement("11"),
                        new DependencyRequirement("com.google.guava", "guava", VersionConstraint.exact("31.0.1"), RequirementStatus.UNKNOWN),
                        new DependencyRequirement("org.slf4j", "slf4j-api", VersionConstraint.any(), RequirementStatus.UNKNOWN)
                )
        );

        ResolutionResult result = resolver.resolve(model);
        assertTrue(result.isSuccessful());
        
        long depEdges = result.getGraph().getEdges().stream()
                .filter(e -> e.getRelationshipType().equals("DEPENDS_ON"))
                .count();
        assertEquals(2, depEdges);
    }

    @Test
    void testIncompatibleVersion() {
        ProjectModel model = new ProjectModel(
                new ProjectMetadata("incompatible-demo", null, null, null),
                Arrays.asList(
                        new JavaRequirement("17"),
                        new DependencyRequirement("some", "dep", VersionConstraint.exact("1.0"), RequirementStatus.UNKNOWN),
                        new DependencyRequirement("some", "dep", VersionConstraint.exact("2.0"), RequirementStatus.UNKNOWN)
                )
        );

        ResolutionResult result = resolver.resolve(model);
        assertFalse(result.isSuccessful());

        RequirementNode depNode = result.getGraph().getNodeById("DEPENDENCY-some:dep").orElseThrow();
        assertEquals(RequirementStatus.INCOMPATIBLE, depNode.getStatus());
    }

    @Test
    void testUnknownRequirement() {
        ProjectModel model = new ProjectModel(
                new ProjectMetadata("unknown-demo", null, null, null),
                Arrays.asList(
                        new JavaRequirement("17"),
                        new DependencyRequirement("weird", "dep", VersionConstraint.exact("invalid@version"), RequirementStatus.UNKNOWN)
                )
        );

        ResolutionResult result = resolver.resolve(model);
        assertFalse(result.isSuccessful());

        RequirementNode depNode = result.getGraph().getNodeById("DEPENDENCY-weird:dep").orElseThrow();
        assertEquals(RequirementStatus.UNKNOWN, depNode.getStatus());
    }
}
