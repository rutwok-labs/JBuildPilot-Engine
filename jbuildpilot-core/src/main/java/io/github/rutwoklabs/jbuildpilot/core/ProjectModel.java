package io.github.rutwoklabs.jbuildpilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable representation of a project's model, containing metadata and all requirements.
 */
public final class ProjectModel {
    private final ProjectMetadata metadata;
    private final List<Requirement> requirements;

    public ProjectModel(ProjectMetadata metadata, List<Requirement> requirements) {
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
        this.requirements = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(requirements, "Requirements cannot be null")
        ));
    }

    public ProjectMetadata getMetadata() {
        return metadata;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    /**
     * Helper to return a new ProjectModel with an updated/added requirement.
     * Keeps the model immutable.
     */
    public ProjectModel withRequirement(Requirement newRequirement) {
        List<Requirement> updatedRequirements = new ArrayList<>(this.requirements);
        // Replace if same identity and type, otherwise add
        boolean replaced = false;
        for (int i = 0; i < updatedRequirements.size(); i++) {
            Requirement req = updatedRequirements.get(i);
            if (req.getType() == newRequirement.getType() && req.getIdentity().equals(newRequirement.getIdentity())) {
                updatedRequirements.set(i, newRequirement);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            updatedRequirements.add(newRequirement);
        }
        return new ProjectModel(this.metadata, updatedRequirements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectModel that = (ProjectModel) o;
        return metadata.equals(that.metadata) && requirements.equals(that.requirements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, requirements);
    }
}
