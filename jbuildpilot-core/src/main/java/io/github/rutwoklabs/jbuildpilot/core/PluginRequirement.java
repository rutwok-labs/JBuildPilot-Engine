package io.github.rutwoklabs.jbuildpilot.core;

import java.util.Objects;

public final class PluginRequirement implements Requirement {
    private final String pluginId; // This could be groupId:artifactId or just a plugin id (like in Gradle)
    private final VersionConstraint versionConstraint;
    private final RequirementStatus status;

    public PluginRequirement(String pluginId, VersionConstraint versionConstraint, RequirementStatus status) {
        this.pluginId = Objects.requireNonNull(pluginId, "Plugin ID cannot be null");
        this.versionConstraint = Objects.requireNonNull(versionConstraint, "Version constraint cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public PluginRequirement(String pluginId, String version) {
        this(pluginId, VersionConstraint.exact(version), RequirementStatus.UNKNOWN);
    }

    @Override
    public String getIdentity() {
        return pluginId;
    }

    @Override
    public RequirementType getType() {
        return RequirementType.PLUGIN;
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
    public PluginRequirement withStatus(RequirementStatus status) {
        return new PluginRequirement(this.pluginId, this.versionConstraint, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginRequirement that = (PluginRequirement) o;
        return pluginId.equals(that.pluginId) && 
               versionConstraint.equals(that.versionConstraint) && 
               status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, versionConstraint, status);
    }
}
