package io.github.rutwoklabs.jbuildpilot.core;

import java.util.Objects;

public final class JavaRequirement implements Requirement {
    private final VersionConstraint versionConstraint;
    private final RequirementStatus status;

    public JavaRequirement(VersionConstraint versionConstraint, RequirementStatus status) {
        this.versionConstraint = Objects.requireNonNull(versionConstraint, "Version constraint cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public JavaRequirement(String version) {
        this(VersionConstraint.exact(version), RequirementStatus.UNKNOWN);
    }

    @Override
    public String getIdentity() {
        return "java";
    }

    @Override
    public RequirementType getType() {
        return RequirementType.JAVA;
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
    public JavaRequirement withStatus(RequirementStatus status) {
        return new JavaRequirement(this.versionConstraint, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaRequirement that = (JavaRequirement) o;
        return versionConstraint.equals(that.versionConstraint) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionConstraint, status);
    }
}
