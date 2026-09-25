package io.github.rutwoklabs.jbuildpilot.core;

import java.util.Objects;

public final class DependencyRequirement implements Requirement {
    private final String groupId;
    private final String artifactId;
    private final VersionConstraint versionConstraint;
    private final RequirementStatus status;

    public DependencyRequirement(String groupId, String artifactId, VersionConstraint versionConstraint, RequirementStatus status) {
        this.groupId = Objects.requireNonNull(groupId, "groupId cannot be null");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId cannot be null");
        this.versionConstraint = Objects.requireNonNull(versionConstraint, "Version constraint cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public DependencyRequirement(String groupId, String artifactId, String version) {
        this(groupId, artifactId, VersionConstraint.exact(version), RequirementStatus.UNKNOWN);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getIdentity() {
        return groupId + ":" + artifactId;
    }

    @Override
    public RequirementType getType() {
        return RequirementType.DEPENDENCY;
    }

    @Override
    public VersionConstraint getVersionConstraint() {
        return versionConstraint;
    }

    @Override
    public RequirementStatus getStatus() {
        return status;
    }

    @Override
    public DependencyRequirement withStatus(RequirementStatus status) {
        return new DependencyRequirement(this.groupId, this.artifactId, this.versionConstraint, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyRequirement that = (DependencyRequirement) o;
        return groupId.equals(that.groupId) && 
               artifactId.equals(that.artifactId) && 
               versionConstraint.equals(that.versionConstraint) && 
               status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, versionConstraint, status);
    }
}
