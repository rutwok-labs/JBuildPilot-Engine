package io.github.rutwoklabs.jbuildpilot.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RequirementGraph {
    private final List<RequirementNode> nodes = new ArrayList<>();
    private final List<RequirementEdge> edges = new ArrayList<>();

    public void addNode(RequirementNode node) {
        nodes.add(node);
    }

    public void addEdge(RequirementEdge edge) {
        edges.add(edge);
    }

    public List<RequirementNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<RequirementEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public Optional<RequirementNode> getNodeById(String id) {
        return nodes.stream().filter(n -> n.getId().equals(id)).findFirst();
    }
}
