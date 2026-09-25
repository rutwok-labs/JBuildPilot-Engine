package io.github.rutwoklabs.jbuildpilot.core;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class ProjectModelTest {

    @Test
    void shouldCreateProjectModel() {
        ProjectMetadata metadata = new ProjectMetadata("test-project", "com.test", "1.0.0", "A test project");
        ProjectModel model = new ProjectModel(metadata, Collections.emptyList());

        assertEquals("test-project", model.getMetadata().getName());
        assertTrue(model.getRequirements().isEmpty());
    }

    @Test
    void shouldAddRequirementImmutably() {
        ProjectMetadata metadata = new ProjectMetadata("test-project", "com.test", "1.0.0", "A test project");
        ProjectModel model = new ProjectModel(metadata, Collections.emptyList());

        JavaRequirement javaReq = new JavaRequirement("21");
        ProjectModel updatedModel = model.withRequirement(javaReq);

        assertTrue(model.getRequirements().isEmpty());
        assertEquals(1, updatedModel.getRequirements().size());
        assertEquals(RequirementType.JAVA, updatedModel.getRequirements().get(0).getType());
    }

    @Test
    void shouldCreateProjectAggregateRoot() {
        ProjectMetadata metadata = new ProjectMetadata("test-project", "com.test", "1.0.0", "");
        ProjectModel model = new ProjectModel(metadata, Collections.emptyList());
        
        Path fakePath = Path.of("/fake/path");
        Project project = new Project(fakePath, model);

        assertEquals(fakePath, project.getProjectDirectory());
        assertEquals(model, project.getModel());
    }
}
