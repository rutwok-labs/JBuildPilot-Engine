package io.github.rutwoklabs.jbuildpilot.core;

import java.util.Objects;

/**
 * Immutable metadata about a project.
 */
public final class ProjectMetadata {
    private final String name;
    private final String groupId;
    private final String version;
    private final String description;

    public ProjectMetadata(String name, String groupId, String version, String description) {
        this.name = name != null ? name : "unnamed-project";
        this.groupId = groupId != null ? groupId : "";
        this.version = version != null ? version : "0.0.0";
        this.description = description != null ? description : "";
    }

    public String getName() {
        return name;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectMetadata that = (ProjectMetadata) o;
        return name.equals(that.name) && 
               groupId.equals(that.groupId) && 
               version.equals(that.version) && 
               description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, groupId, version, description);
    }
}
