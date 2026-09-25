package io.github.rutwoklabs.jbuildpilot.core;

import java.util.Objects;

public final class GradleRequirement implements Requirement {
    private final VersionConstraint versionConstraint;
    private final RequirementStatus status;

    public GradleRequirement(VersionConstraint versionConstraint, RequirementStatus status) {
        this.versionConstraint = Objects.requireNonNull(versionConstraint, "Version constraint cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public GradleRequirement(String version) {
        this(VersionConstraint.exact(version), RequirementStatus.UNKNOWN);
    }

    @Override
    public String getIdentity() {
        return "gradle";
    }

    @Override
    public RequirementType getType() {
        return RequirementType.GRADLE;
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
    public GradleRequirement withStatus(RequirementStatus status) {
        return new GradleRequirement(this.versionConstraint, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GradleRequirement that = (GradleRequirement) o;
        return versionConstraint.equals(that.versionConstraint) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionConstraint, status);
    }
}
