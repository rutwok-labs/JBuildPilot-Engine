package io.github.rutwoklabs.jbuildpilot.resolver;

import io.github.rutwoklabs.jbuildpilot.core.Requirement;
import io.github.rutwoklabs.jbuildpilot.core.RequirementStatus;

public class RequirementNode {
    private final String id;
    private final Requirement requirement;
    private RequirementStatus status;

    public RequirementNode(String id, Requirement requirement, RequirementStatus status) {
        this.id = id;
        this.requirement = requirement;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public void setStatus(RequirementStatus status) {
        this.status = status;
    }
}
