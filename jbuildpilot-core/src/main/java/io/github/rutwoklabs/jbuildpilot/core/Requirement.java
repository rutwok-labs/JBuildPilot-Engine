package io.github.rutwoklabs.jbuildpilot.core;

/**
 * Core interface for any project requirement.
 * All requirements should be immutable.
 */
public interface Requirement {
    /**
     * @return the identity of the requirement (e.g., 'java', 'maven', or 'groupId:artifactId')
     */
    String getIdentity();

    /**
     * @return the type of this requirement
     */
    RequirementType getType();

    /**
     * @return the version constraint to satisfy this requirement
     */
    VersionConstraint getVersionConstraint();

    /**
     * @return the current status of this requirement
     */
    RequirementStatus getStatus();

    /**
     * Returns a copy of this requirement with the updated status.
     * @param status the new status
     * @return a new requirement instance
     */
    Requirement withStatus(RequirementStatus status);
}
