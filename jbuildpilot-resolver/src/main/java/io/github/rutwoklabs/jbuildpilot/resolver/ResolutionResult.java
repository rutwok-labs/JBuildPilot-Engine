package io.github.rutwoklabs.jbuildpilot.resolver;

public class ResolutionResult {
    private final RequirementGraph graph;
    private final boolean successful;

    public ResolutionResult(RequirementGraph graph, boolean successful) {
        this.graph = graph;
        this.successful = successful;
    }

    public RequirementGraph getGraph() {
        return graph;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
