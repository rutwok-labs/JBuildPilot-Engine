package io.github.rutwoklabs.jbuildpilot.core;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The aggregate root representing a project on disk with its analyzed model.
 */
public final class Project {
    private final Path projectDirectory;
    private final ProjectModel model;

    public Project(Path projectDirectory, ProjectModel model) {
        this.projectDirectory = Objects.requireNonNull(projectDirectory, "Project directory cannot be null");
        this.model = Objects.requireNonNull(model, "Project model cannot be null");
    }

    public Path getProjectDirectory() {
        return projectDirectory;
    }

    public ProjectModel getModel() {
        return model;
    }

    /**
     * Returns a new Project instance with the updated model.
     */
    public Project withModel(ProjectModel newModel) {
        return new Project(this.projectDirectory, newModel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return projectDirectory.equals(project.projectDirectory) && model.equals(project.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectDirectory, model);
    }
}
