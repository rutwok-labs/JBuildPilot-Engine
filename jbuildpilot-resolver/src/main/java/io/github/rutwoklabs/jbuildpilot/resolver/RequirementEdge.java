package io.github.rutwoklabs.jbuildpilot.resolver;

public class RequirementEdge {
    private final RequirementNode source;
    private final RequirementNode target;
    private final String relationshipType;

    public RequirementEdge(RequirementNode source, RequirementNode target, String relationshipType) {
        this.source = source;
        this.target = target;
        this.relationshipType = relationshipType;
    }

    public RequirementNode getSource() {
        return source;
    }

    public RequirementNode getTarget() {
        return target;
    }

    public String getRelationshipType() {
        return relationshipType;
    }
}
